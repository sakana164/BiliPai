package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.util.lerp
import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset

internal data class BottomBarInnerRimGlowSpec(
    val radiusDp: Float,
    val alpha: Float
)

internal data class BottomBarShellShaderSpec(
    val thicknessDp: Float,
    val refractIndex: Float,
    val refractIntensity: Float
)

internal data class BottomBarGlassMaterialSpec(
    val blurRadiusDp: Float?,
    val vibrancy: Boolean,
    val shellRefractionHeightDp: Float,
    val shellRefractionAmountDp: Float,
    val shellChromaticAberration: Boolean,
    val foregroundTint: Color,
    val highlightWidthScale: Float,
    val shadowAlphaScale: Float,
    val innerRimGlow: BottomBarInnerRimGlowSpec?,
    val shellShader: BottomBarShellShaderSpec?
)

internal fun resolveBottomBarGlassMaterialSpec(
    preset: BottomBarLiquidGlassPreset,
    isDarkTheme: Boolean,
    isScrolling: Boolean,
    glassEnabled: Boolean,
    motionProgress: Float,
    pressProgress: Float
): BottomBarGlassMaterialSpec {
    if (!glassEnabled) {
        return BottomBarGlassMaterialSpec(
            blurRadiusDp = null,
            vibrancy = false,
            shellRefractionHeightDp = 0f,
            shellRefractionAmountDp = 0f,
            shellChromaticAberration = false,
            foregroundTint = Color.Transparent,
            highlightWidthScale = 1f,
            shadowAlphaScale = 1f,
            innerRimGlow = null,
            shellShader = null
        )
    }
    return when (preset) {
        BottomBarLiquidGlassPreset.BILIPAI_TUNED -> bilipaiTunedBottomBarGlassMaterial()
        BottomBarLiquidGlassPreset.IOS26_REFINED -> ios26BottomBarGlassMaterial(
            isDarkTheme = isDarkTheme,
            isScrolling = isScrolling,
            motionProgress = motionProgress,
            pressProgress = pressProgress
        )
    }
}

internal fun resolveBottomBarGlassMaterialContainerColor(
    surfaceColor: Color,
    preset: BottomBarLiquidGlassPreset,
    glassEnabled: Boolean,
    fallbackAlpha: Float
): Color {
    if (!glassEnabled) return surfaceColor.copy(alpha = fallbackAlpha)
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val alpha = when (preset) {
        BottomBarLiquidGlassPreset.BILIPAI_TUNED -> if (isDarkSurface) 0.30f else 0.38f
        BottomBarLiquidGlassPreset.IOS26_REFINED -> if (isDarkSurface) 0.34f else 0.40f
    }
    return surfaceColor.copy(alpha = alpha)
}

private fun bilipaiTunedBottomBarGlassMaterial(): BottomBarGlassMaterialSpec =
    BottomBarGlassMaterialSpec(
        blurRadiusDp = null,
        vibrancy = true,
        shellRefractionHeightDp = 24f,
        shellRefractionAmountDp = 24f,
        shellChromaticAberration = true,
        foregroundTint = Color.Transparent,
        highlightWidthScale = 1f,
        shadowAlphaScale = 1f,
        innerRimGlow = null,
        shellShader = null
    )

private fun ios26BottomBarGlassMaterial(
    isDarkTheme: Boolean,
    isScrolling: Boolean,
    motionProgress: Float,
    pressProgress: Float
): BottomBarGlassMaterialSpec {
    val activity = maxOf(
        if (isScrolling) 1f else 0f,
        motionProgress.coerceIn(0f, 1f) * 0.45f,
        pressProgress.coerceIn(0f, 1f) * 0.35f
    )
    // 可读性兜底：仅随上下滚动抬高前景层，与 motion/press 无关
    val scrollReadability = if (isScrolling) 1f else 0f
    val readabilityAlpha = lerp(0f, 0.05f, scrollReadability)
    return BottomBarGlassMaterialSpec(
        blurRadiusDp = lerp(7f, 6f, activity),
        vibrancy = false,
        shellRefractionHeightDp = 0f,
        shellRefractionAmountDp = 0f,
        shellChromaticAberration = false,
        foregroundTint = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = readabilityAlpha),
        highlightWidthScale = lerp(1.2f, 1.3f, activity),
        shadowAlphaScale = 0.72f,
        innerRimGlow = BottomBarInnerRimGlowSpec(radiusDp = 5f, alpha = 0.09f),
        shellShader = BottomBarShellShaderSpec(
            thicknessDp = 11f,
            refractIndex = 1.5f,
            refractIntensity = 0.70f
        )
    )
}

internal data class LiquidGlassShaderUniforms(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadiusPx: Float,
    val thicknessPx: Float,
    val refractIndex: Float,
    val refractIntensity: Float,
    val resolutionX: Float,
    val resolutionY: Float
)

internal fun resolveLiquidGlassShaderUniforms(
    widthPx: Float,
    heightPx: Float,
    paddingPx: Float,
    cornerRadiusPx: Float,
    thicknessPx: Float,
    refractIndex: Float,
    refractIntensity: Float,
    intensityScale: Float
): LiquidGlassShaderUniforms {
    val halfWidth = widthPx / 2f
    val halfHeight = heightPx / 2f
    val maxRadius = minOf(halfWidth, halfHeight)
    return LiquidGlassShaderUniforms(
        centerX = paddingPx + halfWidth,
        centerY = paddingPx + halfHeight,
        halfWidth = halfWidth,
        halfHeight = halfHeight,
        cornerRadiusPx = cornerRadiusPx.coerceIn(0f, maxRadius),
        thicknessPx = thicknessPx,
        refractIndex = refractIndex,
        refractIntensity = (refractIntensity * intensityScale).coerceAtLeast(0f),
        resolutionX = widthPx + paddingPx * 2f,
        resolutionY = heightPx + paddingPx * 2f
    )
}
