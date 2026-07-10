package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.LyricDocument

internal fun resolveMusicLiquidGlassEnabled(
    sdkInt: Int,
    effectsEnabled: Boolean,
    isAppInBackground: Boolean,
    reduceMotion: Boolean
): Boolean {
    return effectsEnabled &&
        sdkInt in 33..35 &&
        !isAppInBackground &&
        !reduceMotion
}

internal fun resolveCurrentLyricIndex(
    document: LyricDocument,
    positionMs: Long
): Int {
    if (document.lines.isEmpty()) return -1
    val adjustedPosition = positionMs - document.offsetMs
    var low = 0
    var high = document.lines.lastIndex
    var result = -1
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (document.lines[middle].startTimeMs <= adjustedPosition) {
            result = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return result
}
