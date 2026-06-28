package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass

data class ProfileLayoutTokens(
    val heroHeightFraction: Float = 0.36f,
    val heroMinHeightDp: Int = 280,
    val heroMaxHeightDp: Int = 360,
    val contentSheetTopRadiusDp: Int = 20,
    val contentSheetTopOverlapDp: Int = 20,
    val contentSheetTopPaddingDp: Int = 8,
    val contentSheetBottomPaddingDp: Int = 24,
    val heroBottomInsetDp: Int = 24,
    val sectionSpacingDp: Int = 20,
    val tabHeightDp: Int = 48
)

data class ProfileCardTokens(
    val widthDp: Int = 140,
    val coverAspectRatio: Float = 3f / 4f,
    val cornerRadiusDp: Int = 12,
    val metadataHeightDp: Int = 52,
    val gapDp: Int = 12
)

fun resolveProfileLayoutTokens(): ProfileLayoutTokens = ProfileLayoutTokens()

fun resolveProfileCardTokens(): ProfileCardTokens = ProfileCardTokens()

fun resolveProfileHeroHeightDp(
    screenHeightDp: Int,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
): Float {
    val tokens = resolveProfileLayoutTokens()
    val widthScale = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded,
        WindowWidthSizeClass.Medium -> 0.92f
        WindowWidthSizeClass.Compact -> 1f
    }
    return (screenHeightDp * tokens.heroHeightFraction * widthScale)
        .coerceIn(tokens.heroMinHeightDp.toFloat(), tokens.heroMaxHeightDp.toFloat())
}

fun resolveProfileTopBannerHeightDp(
    widthSizeClass: WindowWidthSizeClass,
    screenHeightDp: Int = referenceProfileScreenHeightDp(widthSizeClass)
): Float {
    return resolveProfileHeroHeightDp(screenHeightDp = screenHeightDp, widthSizeClass = widthSizeClass)
}

internal fun referenceProfileScreenHeightDp(widthSizeClass: WindowWidthSizeClass): Int {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 800
        WindowWidthSizeClass.Medium -> 900
        WindowWidthSizeClass.Expanded -> 1000
    }
}

fun resolveProfileCardCoverHeightDp(cardTokens: ProfileCardTokens = resolveProfileCardTokens()): Float {
    return cardTokens.widthDp / cardTokens.coverAspectRatio
}

fun resolveProfileCardHeightDp(cardTokens: ProfileCardTokens = resolveProfileCardTokens()): Float {
    return resolveProfileCardCoverHeightDp(cardTokens) + cardTokens.metadataHeightDp
}