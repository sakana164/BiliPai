package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Rect

internal data class VideoPredictiveReturnTransform(
    val scaleX: Float,
    val scaleY: Float,
    val translationX: Float,
    val translationY: Float,
    val cornerRadiusDp: Float
) {
    companion object {
        val Identity = VideoPredictiveReturnTransform(
            scaleX = 1f,
            scaleY = 1f,
            translationX = 0f,
            translationY = 0f,
            cornerRadiusDp = 0f
        )
    }
}

internal fun resolveVideoPredictiveReturnTransform(
    rootBounds: Rect?,
    sourceBounds: Rect?,
    progress: Float,
    targetCornerRadiusDp: Float = 16f
): VideoPredictiveReturnTransform {
    val root = rootBounds ?: return VideoPredictiveReturnTransform.Identity
    val source = sourceBounds ?: return VideoPredictiveReturnTransform.Identity
    if (root.width <= 0f || root.height <= 0f || source.width <= 0f || source.height <= 0f) {
        return VideoPredictiveReturnTransform.Identity
    }

    val clampedProgress = progress.coerceIn(0f, 1f)
    val targetScaleX = source.width / root.width
    val targetScaleY = source.height / root.height
    val targetTranslationX = source.center.x - root.center.x
    val targetTranslationY = source.center.y - root.center.y

    return VideoPredictiveReturnTransform(
        scaleX = 1f + (targetScaleX - 1f) * clampedProgress,
        scaleY = 1f + (targetScaleY - 1f) * clampedProgress,
        translationX = targetTranslationX * clampedProgress,
        translationY = targetTranslationY * clampedProgress,
        cornerRadiusDp = targetCornerRadiusDp * clampedProgress
    )
}

