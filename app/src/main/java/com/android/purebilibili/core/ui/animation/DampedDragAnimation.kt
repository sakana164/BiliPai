// 文件路径: core/ui/animation/DampedDragAnimation.kt
package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.android.purebilibili.core.ui.motion.BottomBarMotionSpec
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun resolveDampedDragVelocityItemsPerSecond(
    velocityPxPerSecond: Float,
    itemWidthPx: Float
): Float {
    if (itemWidthPx <= 0f) return 0f
    return velocityPxPerSecond / itemWidthPx
}

/**
 * 共享的 KernelSU 指示器拖拽动画状态。
 *
 * 复刻来源：
 * KernelSU manager FloatingBottomBar / DampedDragAnimation / DragGestureInspector，
 * commit 778fb38bbf0c43f168b8bbd7d9e369d6fb46754b。
 * 底栏、顶部标签、分段控件、分区侧栏共用此内核，避免各自维护一套速度形变。
 */
internal class DampedDragAnimationState(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
    private val onIndexChanged: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") private val motionSpec: BottomBarMotionSpec,
    private val notifyIndexChangedOnReleaseStart: Boolean = false,
    @Suppress("UNUSED_PARAMETER") private val holdPressUntilReleaseTargetSettles: Boolean = false
) {
    private val valueAnimationSpec = spring(1f, 1000f, 0.001f)
    private val velocityAnimationSpec = spring(0.5f, 300f, 0.01f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)
    private val offsetSnapAnimationSpec = spring(1f, 300f, 0.5f)

    private val valueAnimation = Animatable(initialIndex.toFloat(), 0.001f)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(1f, 0.001f)
    private val scaleYAnimation = Animatable(1f, 0.001f)
    private val offsetAnimation = Animatable(0f)
    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    private var motionGeneration = 0
    private var valueJob: Job? = null
    private var velocityJob: Job? = null
    private var releaseJob: Job? = null
    private var offsetJob: Job? = null

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val velocity: Float get() = velocityAnimation.value
    val deformationVelocityItemsPerSecond: Float get() = velocityAnimation.value
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val scale: Float get() = maxOf(scaleX, scaleY)
    val dragOffset: Float get() = offsetAnimation.value
    val isRunning: Boolean get() = valueAnimation.isRunning

    var velocityPxPerSecond by mutableFloatStateOf(0f)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var targetIndex = initialIndex
        private set

    var settledReleaseCount by mutableIntStateOf(0)
        private set

    var settledSelectionCount by mutableIntStateOf(0)
        private set

    private fun startNewMotion(): Int {
        motionGeneration += 1
        return motionGeneration
    }

    fun press() {
        velocityTracker.resetTracking()
        releaseJob?.cancel()
        releaseJob = scope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(KERNEL_SU_PRESSED_SCALE, scaleYAnimationSpec) }
        }
    }

    fun release(onSettled: (() -> Unit)? = null) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = ((itemCount - 1).toFloat() * 0.025f).coerceAtLeast(0.001f)
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            onSettled?.invoke()
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(1f, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(1f, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val nextTarget = value.fastCoerceIn(0f, (itemCount - 1).toFloat())
        valueJob?.cancel()
        valueJob = scope.launch {
            valueAnimation.animateTo(nextTarget, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float, onSettled: (() -> Unit)? = null) {
        scope.launch {
            mutatorMutex.mutate {
                press()
                val nextTarget = value.fastCoerceIn(0f, (itemCount - 1).toFloat())
                targetIndex = nextTarget.roundToInt().coerceIn(0, itemCount - 1)
                valueJob?.cancel()
                valueJob = launch { valueAnimation.animateTo(nextTarget, valueAnimationSpec) }
                if (velocity != 0f) {
                    velocityJob?.cancel()
                    velocityJob = launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release(onSettled = onSettled)
            }
        }
    }

    fun onDrag(
        dragAmountPx: Float,
        itemWidthPx: Float,
        gestureVelocityPxPerSecond: Float = 0f
    ) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        if (!isDragging) {
            isDragging = true
            startNewMotion()
            valueJob?.cancel()
            offsetJob?.cancel()
            velocityPxPerSecond = 0f
            press()
        }
        velocityPxPerSecond = gestureVelocityPxPerSecond
        updateValue(targetValue + dragAmountPx / itemWidthPx)
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.snapTo(offsetAnimation.value + dragAmountPx)
        }
    }

    fun onDragEnd(
        velocityX: Float,
        itemWidthPx: Float,
        settleIndex: Int? = null,
        notifyIndexChanged: Boolean = true
    ) {
        if (itemWidthPx <= 0f || itemCount <= 0) return
        isDragging = false
        val generation = motionGeneration
        velocityPxPerSecond = velocityX
        val releaseTargetIndex = settleIndex?.coerceIn(0, itemCount - 1)
            ?: targetValue.fastRoundToInt().fastCoerceIn(0, itemCount - 1)
        targetIndex = releaseTargetIndex
        if (notifyIndexChanged && notifyIndexChangedOnReleaseStart) {
            onIndexChanged(releaseTargetIndex)
        }
        animateToValue(releaseTargetIndex.toFloat()) {
            if (generation == motionGeneration) {
                velocityPxPerSecond = 0f
                settledReleaseCount += 1
                if (notifyIndexChanged && !notifyIndexChangedOnReleaseStart) {
                    onIndexChanged(releaseTargetIndex)
                }
            }
        }
        offsetJob?.cancel()
        offsetJob = scope.launch {
            offsetAnimation.animateTo(0f, offsetSnapAnimationSpec)
        }
    }

    fun setPressed(pressed: Boolean) {
        if (pressed) {
            press()
        } else if (!isDragging) {
            release()
        }
    }

    fun snapTo(targetValue: Float) {
        val generation = startNewMotion()
        valueJob?.cancel()
        targetIndex = targetValue.roundToInt().coerceIn(0, itemCount - 1)
        scope.launch {
            if (generation != motionGeneration) return@launch
            valueAnimation.stop()
            valueAnimation.snapTo(targetValue)
            velocityAnimation.snapTo(0f)
        }
    }

    fun updateIndex(index: Int) {
        if (isDragging || itemCount <= 0) return
        val safeIndex = index.coerceIn(0, itemCount - 1)
        if (
            safeIndex == targetIndex &&
            (
                isRunning ||
                    abs(value - safeIndex.toFloat()) < 0.005f ||
                    abs(targetValue - safeIndex.toFloat()) < 0.005f
                )
        ) return
        startNewMotion()
        targetIndex = safeIndex
        velocityPxPerSecond = 0f
        animateToValue(safeIndex.toFloat()) {
            settledSelectionCount += 1
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val denominator = (itemCount - 1).toFloat().coerceAtLeast(1f)
        val targetVelocity = velocityTracker.calculateVelocity().x / denominator
        velocityJob?.cancel()
        velocityJob = scope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

private const val KERNEL_SU_PRESSED_SCALE = 78f / 56f

@Composable
internal fun rememberDampedDragAnimationState(
    initialIndex: Int,
    itemCount: Int,
    onIndexChanged: (Int) -> Unit,
    motionSpec: BottomBarMotionSpec = resolveBottomBarMotionSpec(),
    notifyIndexChangedOnReleaseStart: Boolean = false,
    holdPressUntilReleaseTargetSettles: Boolean = false
): DampedDragAnimationState {
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)

    return remember(
        itemCount,
        motionSpec,
        notifyIndexChangedOnReleaseStart,
        holdPressUntilReleaseTargetSettles
    ) {
        DampedDragAnimationState(
            initialIndex = initialIndex,
            itemCount = itemCount,
            scope = scope,
            onIndexChanged = { currentOnIndexChanged(it) },
            motionSpec = motionSpec,
            notifyIndexChangedOnReleaseStart = notifyIndexChangedOnReleaseStart,
            holdPressUntilReleaseTargetSettles = holdPressUntilReleaseTargetSettles
        )
    }
}

internal fun Modifier.horizontalDragGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float,
    consumePointerChanges: Boolean = true,
    settleIndex: Int? = null,
    notifyIndexChanged: Boolean = true
): Modifier = this.pointerInput(
    dragState,
    itemWidthPx,
    consumePointerChanges,
    settleIndex,
    notifyIndexChanged
) {
    val velocityTracker = VelocityTracker()

    awaitPointerEventScope {
        while (true) {
            // BiliPai 的底栏输入层叠在可点击 tab 上，必须允许从已消费的 DOWN 开始识别拖拽。
            val down = awaitFirstDown(requireUnconsumed = false)
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                if (consumePointerChanges) {
                    change.consume()
                }
                dragState.onDrag(over, itemWidthPx)
            }

            if (dragStart != null) {
                velocityTracker.addPosition(dragStart.uptimeMillis, dragStart.position)
                var isCancelled = false

                try {
                    horizontalDrag(dragStart.id) { change ->
                        if (consumePointerChanges) {
                            change.consume()
                        }
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val dragAmount = change.position.x - change.previousPosition.x
                        val velocity = velocityTracker.calculateVelocity()
                        dragState.onDrag(dragAmount, itemWidthPx, velocity.x)
                    }
                } catch (_: Exception) {
                    isCancelled = true
                }

                val velocity = if (isCancelled) {
                    0f
                } else {
                    velocityTracker.calculateVelocity().x
                }
                dragState.onDragEnd(
                    velocityX = velocity,
                    itemWidthPx = itemWidthPx,
                    settleIndex = settleIndex,
                    notifyIndexChanged = notifyIndexChanged
                )
            }
        }
    }
}
