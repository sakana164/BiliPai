package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.parseSplLyrics
import com.android.purebilibili.feature.audio.lyrics.LyricDocument
import com.android.purebilibili.feature.audio.lyrics.LyricLine
import com.android.purebilibili.feature.audio.lyrics.resolveActiveLyricIndex
import com.android.purebilibili.feature.audio.lyrics.resolveLyricFocusScrollOffsetPx
import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerVisualPolicyTest {

    @Test
    fun `pager indicator follows drag and stays inside two segments`() {
        assertEquals(0f, resolveMusicPagerIndicatorPosition(0, -0.4f))
        assertEquals(0.35f, resolveMusicPagerIndicatorPosition(0, 0.35f))
        assertEquals(1f, resolveMusicPagerIndicatorPosition(1, 0.4f))
    }

    @Test
    fun `glass fallback remains legible without refraction`() {
        val fallback = resolveMusicGlassFallbackStyle()

        assertTrue(fallback.backgroundAlphaPercent >= 46)
        assertTrue(fallback.borderAlphaPercent >= 22)
    }

    @Test
    fun `liquid controls require supported foreground renderer`() {
        assertFalse(resolveMusicLiquidGlassEnabled(32, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(33, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(36, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, true, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, false, true))
        assertFalse(resolveMusicLiquidGlassEnabled(35, false, false, false))
    }

    @Test
    fun `play mode segmented indices round trip`() {
        PlayMode.entries.forEach { mode ->
            assertEquals(mode, resolveMusicPlayMode(resolveMusicPlayModeIndex(mode)))
        }
        assertEquals(PlayMode.SEQUENTIAL, resolveMusicPlayMode(-1))
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

        assertEquals(-1, resolveActiveLyricIndex(document, positionMs = 1_000L))
        assertEquals(0, resolveActiveLyricIndex(document, positionMs = 1_600L))
        assertEquals(1, resolveActiveLyricIndex(document, positionMs = 3_500L))
        assertEquals(2, resolveActiveLyricIndex(document, positionMs = 8_000L))
        assertEquals(-1, resolveActiveLyricIndex(document, positionMs = 15_501L))
    }

    @Test
    fun `explicit lyric ending leaves long instrumental gap unfocused`() {
        val document = parseSplLyrics(
            """
            [00:01.00]Short line<00:02.00>
            [00:10.00]After gap
            """.trimIndent()
        )

        assertEquals(0, resolveActiveLyricIndex(document, 1_500L))
        assertEquals(-1, resolveActiveLyricIndex(document, 5_000L))
        assertEquals(1, resolveActiveLyricIndex(document, 10_000L))
    }

    @Test
    fun `overlapping lyrics prefer latest active line`() {
        val document = LyricDocument(
            lines = listOf(
                LyricLine(1_000L, 8_000L, "Long"),
                LyricLine(3_000L, 4_000L, "Short")
            )
        )

        assertEquals(1, resolveActiveLyricIndex(document, 3_500L))
        assertEquals(0, resolveActiveLyricIndex(document, 5_000L))
    }

    @Test
    fun `lyric focus offset scales with viewport instead of density constants`() {
        assertEquals(-300, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 1_000))
        assertEquals(-600, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 2_000))
        assertEquals(0, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 0))
    }

    @Test
    fun `lyric focus keeps current line sharp and progressively blurs distant lines`() {
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 100),
            resolveMusicLyricFocusStyle(lineIndex = 4, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 2, alphaPercent = 46),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 7, alphaPercent = 20),
            resolveMusicLyricFocusStyle(lineIndex = 8, currentIndex = 4, blurEnabled = true)
        )
    }

    @Test
    fun `lyric blur falls back to opacity when renderer or motion policy disables it`() {
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 30, effectsEnabled = true, reduceMotion = false))
        assertTrue(resolveMusicLyricsBlurEnabled(sdkInt = 31, effectsEnabled = true, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = false, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = true, reduceMotion = true))
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 46),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = false)
        )
    }
}
