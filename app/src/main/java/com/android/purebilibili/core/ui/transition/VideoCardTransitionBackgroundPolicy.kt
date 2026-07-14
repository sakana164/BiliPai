package com.android.purebilibili.core.ui.transition

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt

private const val VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX = 36f
private const val VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX = 2f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK = 0.22f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT = 0.10f
private const val VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_DARK = 0.10f
private const val VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_LIGHT = 0.05f
private const val VIDEO_CARD_TRANSITION_LIGHT_REDUCED_OPENING_SCRIM_ALPHA = 0.06f
private const val VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION = 0.045f
private val VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT = Color(0xFF8E8E93)

// 开场与返回时长由共享元素速度设置提供；取消仍固定为短恢复动画。
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS = 460
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS = 160

internal enum class VideoCardTransitionBackgroundPhase {
    IDLE,
    OPENING,
    HELD,
    RETURNING
}

internal data class VideoCardTransitionBackgroundFrame(
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float,
    val useLightScrimTint: Boolean = false,
)

internal data class VideoCardTransitionBackgroundState(
    val progressProvider: () -> Float = { 0f },
    val sourceRouteProvider: () -> String? = { null },
    val phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.IDLE
    },
    val isReturnGestureInProgressProvider: () -> Boolean = { false },
    val isGestureRestoreInProgressProvider: () -> Boolean = { false },
    val motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    val isLightBackgroundProvider: () -> Boolean = { false },
)

internal val LocalVideoCardTransitionBackgroundState = compositionLocalOf {
    VideoCardTransitionBackgroundState()
}

internal fun resolveVideoCardTransitionOpeningScrimAlpha(
    progress: Float,
    isLightBackground: Boolean,
    motionTier: MotionTier,
): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val maxAlpha = when {
        isLightBackground && motionTier == MotionTier.Reduced ->
            VIDEO_CARD_TRANSITION_LIGHT_REDUCED_OPENING_SCRIM_ALPHA
        isLightBackground ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT
        else ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK
    }
    return maxAlpha * clamped
}

internal fun resolveVideoCardTransitionReturningScrimAlpha(
    blurStrength: Float,
    isLightBackground: Boolean,
): Float {
    val maxAlpha = if (isLightBackground) {
        VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_LIGHT
    } else {
        VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_DARK
    }
    return maxAlpha * blurStrength
}

internal fun resolveVideoCardTransitionOpeningContentScale(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    isGestureRestoreInProgress: Boolean,
): Float {
    if (phase != VideoCardTransitionBackgroundPhase.OPENING || isGestureRestoreInProgress) {
        return 1f
    }
    val clamped = progress.coerceIn(0f, 1f)
    return 1f - VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION * clamped
}

internal fun resolveVideoCardTransitionBackgroundFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier = MotionTier.Normal,
    isLightBackground: Boolean = false,
    isGestureRestoreInProgress: Boolean = false,
    sdkInt: Int = Build.VERSION.SDK_INT,
): VideoCardTransitionBackgroundFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    // 低端/省电/无障碍减弱动画(Reduced)时跳过整帧 GPU 实时模糊，仅保留 scrim + 轻微缩放作为回退。
    val rawBlurRadiusPx = if (
        phase != VideoCardTransitionBackgroundPhase.IDLE &&
        motionTier != MotionTier.Reduced &&
        sdkInt >= Build.VERSION_CODES.S
    ) {
        VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX * blurStrength
    } else {
        0f
    }

    return VideoCardTransitionBackgroundFrame(
        blurRadiusPx = quantizeVideoCardTransitionBlurRadius(rawBlurRadiusPx),
        scrimAlpha = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING ->
                resolveVideoCardTransitionOpeningScrimAlpha(
                    progress = clamped,
                    isLightBackground = isLightBackground,
                    motionTier = motionTier,
                )
            VideoCardTransitionBackgroundPhase.RETURNING ->
                resolveVideoCardTransitionReturningScrimAlpha(
                    blurStrength = blurStrength,
                    isLightBackground = isLightBackground,
                )
            VideoCardTransitionBackgroundPhase.IDLE,
            VideoCardTransitionBackgroundPhase.HELD -> 0f
        },
        contentScale = resolveVideoCardTransitionOpeningContentScale(
            progress = clamped,
            phase = phase,
            isGestureRestoreInProgress = isGestureRestoreInProgress,
        ),
        useLightScrimTint = isLightBackground,
    )
}

/**
 * 预测式返回手势进行中时，把系统回退进度(0→1)映射为背景虚化进度(1→0)。
 *
 * - 手势起点(0)保持满虚化，与 [VideoCardTransitionBackgroundPhase.HELD] 无缝衔接；
 * - 拖到底(1)则背景基本清晰，从而让全屏 GPU 模糊随手势实时消退，
 *   与共享元素 morph 落位同步，避免"提交返回后才补一段 460ms 模糊 → 封面高斯模糊+闪烁"。
 */
internal fun resolveVideoCardTransitionBackgroundGestureProgress(
    backProgress: Float
): Float {
    val clamped = backProgress.coerceIn(0f, 1f)
    return 1f - clamped
}

/**
 * [VideoCardTransitionBackgroundPhase.OPENING] 阶段预测式返回：以当前开场虚化进度为起点，
 * 随手势线性消退至清晰。与 HELD 满值起点的 [resolveVideoCardTransitionBackgroundGestureProgress] 区分。
 */
internal fun resolveVideoCardTransitionBackgroundOpeningGestureProgress(
    openingBlurProgress: Float,
    backProgress: Float,
): Float {
    val clampedOpening = openingBlurProgress.coerceIn(0f, 1f)
    val clampedBack = backProgress.coerceIn(0f, 1f)
    return clampedOpening * (1f - clampedBack)
}

internal fun isVideoCardTransitionBackgroundGesturePhase(
    phase: VideoCardTransitionBackgroundPhase,
): Boolean {
    return phase == VideoCardTransitionBackgroundPhase.HELD ||
        phase == VideoCardTransitionBackgroundPhase.OPENING
}

internal fun resolveVideoCardTransitionBackgroundGestureBlurProgress(
    phase: VideoCardTransitionBackgroundPhase,
    currentBlurProgress: Float,
    backProgress: Float,
): Float {
    return when (phase) {
        VideoCardTransitionBackgroundPhase.HELD ->
            resolveVideoCardTransitionBackgroundGestureProgress(backProgress)
        VideoCardTransitionBackgroundPhase.OPENING ->
            resolveVideoCardTransitionBackgroundOpeningGestureProgress(
                openingBlurProgress = currentBlurProgress,
                backProgress = backProgress,
            )
        else -> currentBlurProgress
    }
}

/**
 * 返回动画提交时，若手势已消解部分虚化(startProgress < 1)，剩余 [RETURNING] 动画按比例缩短，
 * 保持与共享元素落位一致的视觉速度，避免手势拖到底后仍补一段完整时长的收尾。
 */
internal fun resolveVideoCardTransitionBackgroundReturnDurationMs(
    startProgress: Float,
    fullDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS,
    minDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
): Int {
    val clamped = startProgress.coerceIn(0f, 1f)
    return (fullDurationMs * clamped).roundToInt().coerceIn(minDurationMs, fullDurationMs)
}

internal fun shouldApplyVideoCardTransitionBackgroundToRoute(
    entryRoute: String?,
    sourceRoute: String?,
    activeMainHostRoute: String?
): Boolean {
    val normalizedEntryRoute = normalizeVideoCardTransitionRoute(entryRoute) ?: return false
    val normalizedSourceRoute = normalizeVideoCardTransitionRoute(sourceRoute) ?: return false
    if (!isVideoCardReturnTargetRoute(normalizedSourceRoute)) return false
    if (normalizedEntryRoute == normalizedSourceRoute) return true
    return normalizedEntryRoute == "main_host" &&
        normalizeVideoCardTransitionRoute(activeMainHostRoute) == normalizedSourceRoute
}

/**
 * 视频卡片过渡期间 Nav 层全屏 backdrop：填补 sharedBounds morph / 预测式返回
 * 在屏幕边缘露出的窗口底色，视觉上延续首页虚化后的色调。
 */
internal data class VideoCardTransitionNavBackdropFrame(
    val scrimAlpha: Float,
    val useLightScrimTint: Boolean,
)

internal fun shouldShowVideoCardTransitionNavBackdrop(
    cardTransitionEnabled: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    isVideoDetailOnStack: Boolean,
    isReturningToVideoDetail: Boolean = false,
): Boolean {
    if (!cardTransitionEnabled || !isVideoDetailOnStack || isReturningToVideoDetail) return false
    return phase == VideoCardTransitionBackgroundPhase.HELD ||
        phase == VideoCardTransitionBackgroundPhase.OPENING
}

internal fun resolveVideoCardTransitionNavBackdropFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    isLightBackground: Boolean,
): VideoCardTransitionNavBackdropFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    val scrimAlpha = when (phase) {
        VideoCardTransitionBackgroundPhase.OPENING ->
            resolveVideoCardTransitionOpeningScrimAlpha(
                progress = clamped,
                isLightBackground = isLightBackground,
                motionTier = MotionTier.Normal,
            )
        VideoCardTransitionBackgroundPhase.HELD -> {
            if (isLightBackground) {
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT * blurStrength
            } else {
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK * blurStrength
            }
        }
        else -> 0f
    }
    return VideoCardTransitionNavBackdropFrame(
        scrimAlpha = scrimAlpha,
        useLightScrimTint = isLightBackground,
    )
}

internal fun resolveVideoCardTransitionNavBackdropColor(
    baseBackgroundColor: Color,
    frame: VideoCardTransitionNavBackdropFrame,
): Color {
    if (frame.scrimAlpha <= 0.001f) return baseBackgroundColor
    val tint = if (frame.useLightScrimTint) {
        VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
    } else {
        Color.Black
    }
    return lerp(
        start = baseBackgroundColor,
        stop = tint,
        fraction = frame.scrimAlpha.coerceIn(0f, 1f),
    )
}

/**
 * 每帧内 graphicsLayer 与 drawWithContent 会先后读取同一 frame，
 * 用一个基于 (progress, phase) 的一次性缓存避免同帧重复计算纯函数。
 */
private class VideoCardTransitionBackgroundFrameCache {
    private var lastProgress = Float.NaN
    private var lastPhase: VideoCardTransitionBackgroundPhase? = null
    private var lastMotionTier: MotionTier? = null
    private var lastIsLightBackground: Boolean? = null
    private var lastGestureRestoreInProgress: Boolean? = null
    private var cached = VideoCardTransitionBackgroundFrame(
        blurRadiusPx = 0f,
        scrimAlpha = 0f,
        contentScale = 1f,
    )

    fun resolve(
        progress: Float,
        phase: VideoCardTransitionBackgroundPhase,
        motionTier: MotionTier,
        isLightBackground: Boolean,
        isGestureRestoreInProgress: Boolean,
    ): VideoCardTransitionBackgroundFrame {
        if (
            progress != lastProgress ||
            phase != lastPhase ||
            motionTier != lastMotionTier ||
            isLightBackground != lastIsLightBackground ||
            isGestureRestoreInProgress != lastGestureRestoreInProgress
        ) {
            lastProgress = progress
            lastPhase = phase
            lastMotionTier = motionTier
            lastIsLightBackground = isLightBackground
            lastGestureRestoreInProgress = isGestureRestoreInProgress
            cached = resolveVideoCardTransitionBackgroundFrame(
                progress = progress,
                phase = phase,
                motionTier = motionTier,
                isLightBackground = isLightBackground,
                isGestureRestoreInProgress = isGestureRestoreInProgress,
            )
        }
        return cached
    }
}

internal fun Modifier.videoCardTransitionBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    isGestureRestoreInProgressProvider: () -> Boolean = { false },
    motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    isLightBackgroundProvider: () -> Boolean = { false },
): Modifier {
    val frameCache = VideoCardTransitionBackgroundFrameCache()
    return graphicsLayer {
        val frame = frameCache.resolve(
            progressProvider(),
            phaseProvider(),
            motionTierProvider(),
            isLightBackgroundProvider(),
            isGestureRestoreInProgressProvider(),
        )
        transformOrigin = TransformOrigin(0.5f, 0.5f)
        translationX = 0f
        scaleX = frame.contentScale
        scaleY = frame.contentScale
        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && frame.blurRadiusPx > 0.01f) {
            RenderEffect
                .createBlurEffect(
                    frame.blurRadiusPx,
                    frame.blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                .asComposeRenderEffect()
        } else {
            null
        }
    }.drawWithContent {
        drawContent()
        val frame = frameCache.resolve(
            progressProvider(),
            phaseProvider(),
            motionTierProvider(),
            isLightBackgroundProvider(),
            isGestureRestoreInProgressProvider(),
        )
        if (frame.scrimAlpha > 0.001f) {
            val scrimColor = if (frame.useLightScrimTint) {
                VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
            } else {
                Color.Black
            }
            drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
        }
    }
}

private fun resolveVideoCardTransitionBlurStrength(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    // 模糊要比 scrim 更早进入可感知区，否则 160ms 内肉眼很难看出背景虚化。
    return 1f - (1f - clamped) * (1f - clamped)
}

private fun quantizeVideoCardTransitionBlurRadius(radiusPx: Float): Float {
    if (radiusPx <= 0f) return 0f
    return ((radiusPx / VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX).roundToInt() *
        VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX)
        .coerceIn(0f, VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX)
}

private fun normalizeVideoCardTransitionRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        "home"
    } else {
        normalized.substringBefore("?")
    }
}
