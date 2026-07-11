package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LyricsMatchingPolicyTest {

    private val query = LyricQuery(
        title = "【四月是你的谎言】若能绽放光芒",
        artist = "Goose house",
        durationMs = 258_000L
    )

    @Test
    fun `exact normalized metadata receives full score`() {
        val candidate = LyricCandidate(
            source = LyricSource.NETEASE,
            remoteId = "1",
            title = "若能绽放光芒",
            artist = "Goose House",
            durationMs = 258_000L
        )

        assertEquals(1.0, scoreLyricCandidate(query, candidate), absoluteTolerance = 0.0001)
    }

    @Test
    fun `best candidate balances title artist and duration`() {
        val best = LyricCandidate(
            source = LyricSource.QQ_MUSIC,
            remoteId = "best",
            title = "若能绽放光芒",
            artist = "Goose house",
            durationMs = 260_000L
        )
        val wrongArtist = best.copy(remoteId = "wrong", artist = "Unknown singer")

        assertEquals(best, selectBestLyricCandidate(query, listOf(wrongArtist, best)))
    }

    @Test
    fun `candidate outside duration tolerance is rejected`() {
        val candidate = LyricCandidate(
            source = LyricSource.NETEASE,
            remoteId = "live",
            title = "若能绽放光芒",
            artist = "Goose house",
            durationMs = 300_000L
        )

        assertNull(selectBestLyricCandidate(query, listOf(candidate)))
    }

    @Test
    fun `low confidence candidate is rejected`() {
        val candidate = LyricCandidate(
            source = LyricSource.KUGOU,
            remoteId = "other",
            title = "Completely different",
            artist = "Someone else",
            durationMs = 258_000L
        )

        assertNull(selectBestLyricCandidate(query, listOf(candidate)))
        assertTrue(scoreLyricCandidate(query, candidate) < LYRIC_MATCH_MINIMUM_SCORE)
    }

    @Test
    fun `provider priority resolves otherwise equal candidates`() {
        val base = LyricCandidate(
            source = LyricSource.KUGOU,
            remoteId = "kugou",
            title = "若能绽放光芒",
            artist = "Goose house",
            durationMs = 258_000L
        )
        val qq = base.copy(source = LyricSource.QQ_MUSIC, remoteId = "qq")
        val netease = base.copy(source = LyricSource.NETEASE, remoteId = "netease")

        assertEquals(netease, selectBestLyricCandidate(query, listOf(base, qq, netease)))
    }

    @Test
    fun `quoted song and performer are extracted from noisy video title`() {
        val noisyVideo = LyricQuery(
            title = "【4K修复】超甜！周杰伦《可爱女人》官方现场版",
            artist = "zyl2012_音乐无限",
            durationMs = 213_000L
        )
        val candidate = LyricCandidate(
            source = LyricSource.NETEASE,
            remoteId = "cute-lady",
            title = "可爱女人",
            artist = "周杰伦",
            durationMs = 215_000L
        )

        assertEquals(candidate, selectBestLyricCandidate(noisyVideo, listOf(candidate)))
    }
}
