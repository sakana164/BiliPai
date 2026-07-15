// 文件路径: feature/home/components/LiquidIndicator.kt
// Pure policy/math helpers for liquid indicator geometry and lens profiles.
// Composable Kyant/drawBackdrop paths removed (InstallerX/Miuix stack owns rendering).
package com.android.purebilibili.feature.home.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.LiquidGlassStyle
import kotlin.math.abs
// LiquidGlassTuning lives in this package (LiquidGlassTuning.kt).

internal fun resolveTopTabIndicatorWidthPx(
    itemWidthPx: Float,
    widthRatio: Float,
    minWidthPx: Float,
    horizontalInsetPx: Float
): Float {
    if (itemWidthPx <= 0f) return 0f
    val minBound = minWidthPx.coerceAtMost(itemWidthPx)
    val maxWidth = (itemWidthPx - horizontalInsetPx).coerceAtLeast(minBound)
    val desired = itemWidthPx * widthRatio
    return desired.coerceIn(minBound, maxWidth)
}

internal fun resolveLiquidIndicatorWidthPx(
    itemWidthPx: Float,
    widthMultiplier: Float,
    minWidthPx: Float,
    maxWidthPx: Float,
    maxWidthToItemRatio: Float = Float.POSITIVE_INFINITY
): Float {
    if (itemWidthPx <= 0f) return 0f

    val desiredWidth = itemWidthPx * widthMultiplier
    val designMaxWidth = maxWidthPx.coerceAtLeast(0f)
    val ratioCapWidth = if (maxWidthToItemRatio.isFinite() && maxWidthToItemRatio > 0f) {
        itemWidthPx * maxWidthToItemRatio
    } else {
        Float.POSITIVE_INFINITY
    }
    val effectiveMaxWidth = minOf(designMaxWidth, ratioCapWidth)
    val effectiveMinWidth = minWidthPx.coerceAtLeast(0f).coerceAtMost(effectiveMaxWidth)
    return desiredWidth.coerceIn(effectiveMinWidth, effectiveMaxWidth)
}

internal fun resolveIndicatorTranslationXPx(
    position: Float,
    itemWidthPx: Float,
    indicatorWidthPx: Float,
    startPaddingPx: Float,
    containerWidthPx: Float,
    clampToBounds: Boolean,
    edgeInsetPx: Float,
    viewportShiftPx: Float = 0f
): Float {
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    val raw = startPaddingPx + position * itemWidthPx + centerOffsetPx
    if (!clampToBounds) return raw

    val minX = edgeInsetPx.coerceAtLeast(0f) + viewportShiftPx
    val maxX = (containerWidthPx - indicatorWidthPx - edgeInsetPx + viewportShiftPx).coerceAtLeast(minX)
    return raw.coerceIn(minX, maxX)
}

internal data class LiquidLensProfile(
    val shouldRefract: Boolean,
    val motionFraction: Float,
    val refractionAmount: Float,
    val refractionHeight: Float,
    val centerHighlightAlpha: Float,
    val edgeCompressionAlpha: Float,
    val aberrationStrength: Float
)

internal data class LiquidStyleTuning(
    val idleThresholdPxPerSecond: Float,
    val dragMotionFloor: Float,
    val lensIntensityMultiplier: Float,
    val edgeWarpMultiplier: Float,
    val chromaticMultiplier: Float,
    val deformationMultiplier: Float,
    val idleBlurRadius: Float,
    val depthEffectEnabled: Boolean,
    val allowChromaticAberration: Boolean
)

internal fun resolveLiquidStyleTuning(tuning: LiquidGlassTuning): LiquidStyleTuning =
    when (tuning.mode) {
        LiquidGlassMode.CLEAR -> LiquidStyleTuning(
            idleThresholdPxPerSecond = 150f,
            dragMotionFloor = 0.10f,
            lensIntensityMultiplier = blendFloat(1.18f, 1.42f, tuning.strength),
            edgeWarpMultiplier = blendFloat(1.16f, 1.38f, tuning.strength),
            chromaticMultiplier = blendFloat(0.88f, 1.04f, tuning.strength),
            deformationMultiplier = 0.70f + tuning.strength * 0.14f,
            idleBlurRadius = tuning.backdropBlurRadius,
            depthEffectEnabled = true,
            allowChromaticAberration = false
        )
        LiquidGlassMode.BALANCED -> LiquidStyleTuning(
            idleThresholdPxPerSecond = 120f,
            dragMotionFloor = 0.24f + tuning.strength * 0.10f,
            lensIntensityMultiplier = blendFloat(1.42f, 1.72f, tuning.strength),
            edgeWarpMultiplier = blendFloat(1.40f, 1.78f, tuning.strength),
            chromaticMultiplier = blendFloat(1.08f, 1.42f, tuning.strength),
            deformationMultiplier = 0.92f + tuning.strength * 0.14f,
            idleBlurRadius = tuning.backdropBlurRadius,
            depthEffectEnabled = true,
            allowChromaticAberration = tuning.chromaticAberrationAmount > 0.01f
        )
        LiquidGlassMode.FROSTED -> LiquidStyleTuning(
            idleThresholdPxPerSecond = 220f,
            dragMotionFloor = 0.08f,
            lensIntensityMultiplier = blendFloat(1.00f, 1.16f, tuning.strength),
            edgeWarpMultiplier = blendFloat(0.98f, 1.14f, tuning.strength),
            chromaticMultiplier = 0.82f,
            deformationMultiplier = 0.42f + tuning.strength * 0.06f,
            idleBlurRadius = tuning.backdropBlurRadius,
            depthEffectEnabled = false,
            allowChromaticAberration = false
        )
    }

internal fun resolveLiquidStyleTuning(style: LiquidGlassStyle): LiquidStyleTuning =
    resolveLiquidStyleTuning(resolveLiquidGlassTuning(style))

internal fun resolveLiquidLensProfile(
    isDragging: Boolean,
    velocityPxPerSecond: Float,
    idleThresholdPxPerSecond: Float = 110f,
    dragMotionFloor: Float = 0.22f,
    lensIntensityBoost: Float = 1f,
    edgeWarpBoost: Float = 1f,
    chromaticBoost: Float = 1f,
    velocityRangePxPerSecond: Float = 2600f
): LiquidLensProfile {
    val speed = abs(velocityPxPerSecond)
    val threshold = idleThresholdPxPerSecond
    val safeVelocityRange = velocityRangePxPerSecond.coerceAtLeast(1f)
    val safeDragFloor = dragMotionFloor.coerceIn(0f, 0.8f)
    val safeLensBoost = lensIntensityBoost.coerceIn(0.8f, 2.2f)
    val safeEdgeWarpBoost = edgeWarpBoost.coerceIn(0.8f, 2.2f)
    val safeChromaBoost = chromaticBoost.coerceIn(0.8f, 2.2f)
    val baseMotion = if (isDragging) safeDragFloor else 0f
    val speedMotion = if (isDragging) {
        (speed / safeVelocityRange).coerceIn(0f, 1f)
    } else {
        ((speed - threshold).coerceAtLeast(0f) / safeVelocityRange).coerceIn(0f, 1f)
    }
    val motionFraction = (baseMotion + speedMotion * (1f - baseMotion)).coerceIn(0f, 1f)
    val shouldRefract = isDragging || speed > threshold

    if (!shouldRefract) {
        return LiquidLensProfile(
            shouldRefract = false,
            motionFraction = 0f,
            refractionAmount = 0f,
            refractionHeight = 0f,
            centerHighlightAlpha = 0f,
            edgeCompressionAlpha = 0f,
            aberrationStrength = 0f
        )
    }

    val eased = motionFraction * motionFraction * (3f - 2f * motionFraction)
    return LiquidLensProfile(
        shouldRefract = true,
        motionFraction = motionFraction,
        refractionAmount = (58f + eased * 54f) * safeLensBoost,
        refractionHeight = (84f + eased * 96f) * (0.9f + safeLensBoost * 0.1f),
        centerHighlightAlpha = 0.12f + eased * 0.16f,
        edgeCompressionAlpha = (0.06f + eased * 0.16f) * safeEdgeWarpBoost,
        aberrationStrength = ((0.008f + eased * 0.024f) * safeChromaBoost).coerceIn(0f, 0.06f)
    )
}

private fun DrawScope.drawLiquidSphereSurface(
    baseColor: Color,
    lensProfile: LiquidLensProfile,
    tuning: LiquidGlassTuning
) {
    val isMoving = lensProfile.shouldRefract
    val clearWeight = (1f - tuning.progress).coerceIn(0f, 1f)
    val frostWeight = tuning.progress.coerceIn(0f, 1f)
    val centerGlowAlpha = blendFloat(
        start = if (isMoving) lensProfile.centerHighlightAlpha else 0.12f,
        stop = tuning.whiteOverlayAlpha * 0.52f,
        fraction = frostWeight
    )
    val edgeShadeAlpha = blendFloat(
        start = if (isMoving) lensProfile.edgeCompressionAlpha else 0.03f,
        stop = 0.03f,
        fraction = frostWeight
    )
    val baseAlpha = blendFloat(
        start = if (isMoving) tuning.surfaceAlpha * 0.46f else tuning.surfaceAlpha * 0.58f,
        stop = tuning.surfaceAlpha,
        fraction = frostWeight
    )

    drawRect(baseColor.copy(alpha = baseAlpha))

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = centerGlowAlpha),
                Color.White.copy(alpha = centerGlowAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(x = size.width / 2f, y = size.height * 0.54f),
            radius = size.minDimension * 0.9f
        )
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = edgeShadeAlpha),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = edgeShadeAlpha)
            )
        )
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(
                    alpha = blendFloat(
                        start = if (isMoving) 0.10f else 0.06f,
                        stop = tuning.whiteOverlayAlpha * 1.2f,
                        fraction = frostWeight
                    )
                ),
                Color.Transparent,
                Color.Black.copy(alpha = if (isMoving) 0.09f else 0.04f)
            )
        )
    )

    val ringAlpha = clearWeight * if (isMoving) 0.22f else 0.16f
    if (ringAlpha > 0.01f) {
        val ringStroke = (size.minDimension * 0.05f).coerceAtLeast(1f)
        val ringHighlight = lerp(baseColor, Color.White, 0.48f).copy(alpha = ringAlpha)
        val ringMid = lerp(baseColor, Color.White, 0.22f).copy(alpha = ringAlpha * 0.86f)
        val ringShadow = lerp(baseColor, Color.Black, 0.24f).copy(alpha = ringAlpha * 0.70f)
        drawRoundRect(
            brush = Brush.sweepGradient(
                colors = listOf(
                    ringHighlight,
                    ringMid,
                    ringShadow,
                    ringMid,
                    ringHighlight
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            ),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
            style = Stroke(width = ringStroke)
        )
    }

    if (isMoving && lensProfile.aberrationStrength > 0f && tuning.chromaticAberrationAmount > 0f) {
        val fringe = (lensProfile.aberrationStrength * 3.2f * clearWeight).coerceIn(0f, 0.18f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    lerp(baseColor, Color.White, 0.45f).copy(alpha = fringe),
                    Color.Transparent,
                    Color.Transparent,
                    lerp(baseColor, Color.Black, 0.18f).copy(alpha = fringe)
                )
            )
        )
    }
}

private fun blendFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
