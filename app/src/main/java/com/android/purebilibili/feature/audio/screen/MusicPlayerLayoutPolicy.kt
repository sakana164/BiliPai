package com.android.purebilibili.feature.audio.screen

internal const val MUSIC_PLAYER_EXPANDED_WIDTH_DP = 840

internal enum class MusicPlayerLayout {
    COMPACT_PAGER,
    EXPANDED_SPLIT,
    PIP_ARTWORK
}

internal fun resolveMusicPlayerLayout(
    widthDp: Int,
    isInPipMode: Boolean
): MusicPlayerLayout = when {
    isInPipMode -> MusicPlayerLayout.PIP_ARTWORK
    widthDp >= MUSIC_PLAYER_EXPANDED_WIDTH_DP -> MusicPlayerLayout.EXPANDED_SPLIT
    else -> MusicPlayerLayout.COMPACT_PAGER
}

internal fun resolveMusicArtworkSizeDp(
    availableWidthDp: Int,
    availableHeightDp: Int,
    layout: MusicPlayerLayout
): Int {
    if (availableWidthDp <= 0 || availableHeightDp <= 0) return 0
    return when (layout) {
        MusicPlayerLayout.COMPACT_PAGER -> minOf(
            (availableWidthDp - 48).coerceAtLeast(0),
            (availableHeightDp - 80).coerceAtLeast(0),
            320
        )
        MusicPlayerLayout.EXPANDED_SPLIT -> minOf(
            (availableWidthDp / 2 - 64).coerceAtLeast(0),
            (availableHeightDp - 160).coerceAtLeast(0),
            420
        )
        MusicPlayerLayout.PIP_ARTWORK -> minOf(availableWidthDp, availableHeightDp)
    }
}
