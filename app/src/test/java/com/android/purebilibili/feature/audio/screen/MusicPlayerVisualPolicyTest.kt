package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.parseSplLyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerVisualPolicyTest {

    @Test
    fun `liquid controls require supported foreground renderer`() {
        assertFalse(resolveMusicLiquidGlassEnabled(32, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(33, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(36, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, true, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, false, true))
        assertFalse(resolveMusicLiquidGlassEnabled(35, false, false, false))
    }

    @Test
    fun `current lyric line follows offset adjusted playback time`() {
        val document = parseSplLyrics(
            """
            [00:01.00]One
            [00:03.00]Two
            [00:05.00]Three
            """.trimIndent()
        ).withOffset(500L)

        assertEquals(-1, resolveCurrentLyricIndex(document, positionMs = 1_000L))
        assertEquals(0, resolveCurrentLyricIndex(document, positionMs = 1_600L))
        assertEquals(1, resolveCurrentLyricIndex(document, positionMs = 3_500L))
        assertEquals(2, resolveCurrentLyricIndex(document, positionMs = 8_000L))
    }
}
