package com.android.purebilibili.feature.home.components

/**
 * 顶部标签液态玻璃与 [HomeTopTabChrome] dock 外壳的职责划分。
 *
 * 当 [hasOuterChromeSurface] 为 true 时，外层 dock 已通过
 * [Modifier.homeTopBottomBarMatchedSurface] 完成 shell 级 blur/lens；
 * 内层分段控件只负责指示器、导出采样和交互，避免重复 drawBackdrop。
 */
internal fun shouldTopTabUseLiquidSegmentedControl(
    isLiquidGlassEnabled: Boolean,
    skinPlainStyle: Boolean,
    hasSkinStickerIcons: Boolean,
    forceMaterialUnderline: Boolean
): Boolean {
    return isLiquidGlassEnabled &&
        !skinPlainStyle &&
        !hasSkinStickerIcons &&
        !forceMaterialUnderline
}

internal fun shouldTopTabDrawSegmentedContainerShell(
    liquidGlassEnabled: Boolean,
    hasOuterChromeSurface: Boolean
): Boolean {
    return liquidGlassEnabled && !hasOuterChromeSurface
}

internal fun shouldTopTabDrawSegmentedCaptureBackdropEffects(
    liquidGlassEnabled: Boolean,
    hasOuterChromeSurface: Boolean
): Boolean {
    return liquidGlassEnabled && !hasOuterChromeSurface
}

internal fun resolveTopTabLiquidIndicatorPosition(
    pagerPosition: Float,
    dragPosition: Float,
    dragActive: Boolean,
    pagerInteractionActive: Boolean
): Float? {
    if (dragActive) return null
    if (!pagerInteractionActive) return null
    return pagerPosition
}

internal fun isTopTabPagerInteractionActive(
    pagerIsDragging: Boolean,
    pagerIsScrolling: Boolean
): Boolean {
    return pagerIsDragging || pagerIsScrolling
}