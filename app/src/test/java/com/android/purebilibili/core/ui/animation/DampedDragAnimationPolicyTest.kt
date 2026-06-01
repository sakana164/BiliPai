package com.android.purebilibili.core.ui.animation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DampedDragAnimationPolicyTest {

    @Test
    fun `velocity conversion guards invalid item width`() {
        assertEquals(
            0f,
            resolveDampedDragVelocityItemsPerSecond(
                velocityPxPerSecond = 1200f,
                itemWidthPx = 0f
            )
        )
    }

    @Test
    fun `shared drag animation mirrors KernelSU gesture and release target`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val dragSource = source
            .substringAfter("fun onDrag(")
            .substringBefore("fun onDragEnd(")
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun setPressed(pressed: Boolean)")

        assertTrue(source.contains("private const val KERNEL_SU_PRESSED_SCALE = 78f / 56f"))
        assertTrue(source.contains("private val valueAnimationSpec = spring(1f, 1000f, 0.001f)"))
        assertTrue(source.contains("private val velocityAnimationSpec = spring(0.5f, 300f, 0.01f)"))
        assertTrue(dragSource.contains("updateValue(targetValue + dragAmountPx / itemWidthPx)"))
        assertTrue(dragSource.contains("offsetAnimation.snapTo(offsetAnimation.value + dragAmountPx)"))
        assertTrue(releaseSource.contains("targetValue.fastRoundToInt().fastCoerceIn(0, itemCount - 1)"))
        assertTrue(releaseSource.contains("offsetAnimation.animateTo(0f"))
        assertTrue(source.contains("awaitFirstDown(requireUnconsumed = false)"))
        assertTrue(source.contains("awaitHorizontalTouchSlopOrCancellation(down.id)"))
        assertTrue(source.contains("horizontalDrag(dragStart.id)"))
        assertFalse(source.contains("overscrollLimitItems"))
        assertFalse(source.contains("resolveDampedDragReleaseTargetIndex("))
    }

    @Test
    fun `drag velocity uses KernelSU value tracker for indicator deformation`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()

        assertTrue(source.contains("val deformationVelocityItemsPerSecond: Float get() = velocityAnimation.value"))
        assertTrue(source.contains("velocityTracker.addPosition("))
        assertTrue(source.contains("velocityTracker.calculateVelocity().x"))
        assertTrue(source.contains("velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)"))
        assertFalse(source.contains("deformationVelocityAnimation"))
        assertFalse(source.contains("dragVelocityItemsPerSecond"))
    }

    @Test
    fun `settle pulse counters distinguish drag release and click selection`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val releaseSource = source
            .substringAfter("fun onDragEnd(")
            .substringBefore("fun setPressed(pressed: Boolean)")
        val updateIndexSource = source
            .substringAfter("fun updateIndex(index: Int)")
            .substringBefore("private fun updateVelocity()")

        assertTrue(source.contains("var settledReleaseCount by mutableIntStateOf(0)"))
        assertTrue(source.contains("var settledSelectionCount by mutableIntStateOf(0)"))
        assertTrue(releaseSource.contains("settledReleaseCount += 1"))
        assertFalse(updateIndexSource.contains("settledReleaseCount += 1"))
        assertTrue(updateIndexSource.contains("settledSelectionCount += 1"))
        assertFalse(releaseSource.contains("settledSelectionCount += 1"))
    }

    @Test
    fun `click index update keeps press progress until target settles`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/animation/DampedDragAnimation.kt")
        ).first { it.exists() }.readText()
        val updateIndexSource = source
            .substringAfter("fun updateIndex(index: Int)")
            .substringBefore("private fun updateVelocity()")

        assertTrue(updateIndexSource.contains("animateToValue(safeIndex.toFloat())"))
        assertTrue(source.contains("fun animateToValue(value: Float, onSettled: (() -> Unit)? = null)"))
        assertTrue(source.contains("press()"))
        assertTrue(source.contains("release(onSettled = onSettled)"))
    }
}
