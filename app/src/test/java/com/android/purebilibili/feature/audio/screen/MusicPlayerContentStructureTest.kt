package com.android.purebilibili.feature.audio.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MusicPlayerContentStructureTest {

    @Test
    fun `compact player exposes liquid play lyrics segmented control`() {
        val source = loadSource()
        val compactBranch = source
            .substringAfter("MusicPlayerLayout.COMPACT_PAGER ->")
            .substringBefore("MusicPlayerLayout.EXPANDED_SPLIT ->")

        assertTrue(compactBranch.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(compactBranch.contains("listOf(\"播放\", \"歌词\")"))
        assertTrue(compactBranch.contains("indicatorPositionProvider"))
        assertTrue(compactBranch.contains("animateScrollToPage"))
        assertTrue(compactBranch.contains("navigationBarsPadding()"))
        assertTrue(compactBranch.contains("containerColorOverride = backgroundColor.copy("))
    }

    @Test
    fun `lyrics browsing pauses auto follow without seeking playback`() {
        val source = loadSource()
        val lyricsPage = source.substringAfter("private fun LyricsPage(")

        assertTrue(lyricsPage.contains("collectIsDraggedAsState()"))
        assertTrue(lyricsPage.contains("resolveLyricFocusScrollOffsetPx("))
        assertTrue(lyricsPage.contains("回到当前歌词"))
        assertTrue(!lyricsPage.contains("resolveDraggedLyricIndex("))
        assertTrue(!lyricsPage.contains("snapshotFlow"))
        assertTrue(!lyricsPage.contains("LYRIC_AUTO_FOLLOW_RESUME_DELAY_MS"))
        assertTrue(!lyricsPage.contains("scrollToItem(currentIndex, -160)"))
        assertTrue(!lyricsPage.contains("animateScrollToItem(currentIndex, -160)"))
        assertTrue(source.contains("progressSeekRevision"))
        assertTrue(lyricsPage.contains("LaunchedEffect(progressSeekRevision)"))
    }

    @Test
    fun `player uses three mode liquid dock and moves secondary actions to sheet`() {
        val source = loadSource()
        val playerPage = source
            .substringAfter("private fun PlayerPage(")
            .substringBefore("private fun MusicArtwork(")

        assertTrue(playerPage.contains("MusicPlayModeDock("))
        assertTrue(source.contains("listOf(\"顺序播放\", \"随机播放\", \"单曲循环\")"))
        assertTrue(source.contains("showActions"))
        assertTrue(source.contains("播放器操作"))
        assertTrue(source.contains("onCollectionClick"))
    }

    @Test
    fun `lyrics expose progress playback controls and immersive chrome`() {
        val source = loadSource()
        val lyricsPage = source.substringAfter("private fun LyricsPage(")
        val topBar = source
            .substringAfter("private fun MusicTopBar(")
            .substringBefore("private fun GlassIconButton(")

        assertTrue(lyricsPage.contains("MusicProgress("))
        assertTrue(lyricsPage.contains("PlaybackControls("))
        assertTrue(lyricsPage.contains("AnimatedVisibility("))
        assertTrue(lyricsPage.contains("LyricsImmersiveProgress("))
        assertTrue(lyricsPage.contains("歌词设置"))
        assertTrue(lyricsPage.contains("bottom = 260.dp"))
        assertTrue(lyricsPage.contains("歌词加载失败"))
        assertTrue(lyricsPage.contains("未找到匹配歌词"))
        assertTrue(topBar.contains("CupertinoIcons.Outlined.ChevronDown"))
        assertTrue(topBar.contains("CupertinoIcons.Outlined.Ellipsis"))
        assertTrue(source.contains(".musicGlassSurface(glassEnabled, CircleShape, glassTintColor, miuixBackdrop)"))
        assertTrue(!topBar.contains("?: Spacer"))

        val glassSurface = source.substringAfter("private fun Modifier.musicGlassSurface(")
        assertTrue(glassSurface.contains("kernelSuMiuixFloatingDockSurface("))
        assertTrue(glassSurface.contains("containerColor = containerColor"))
        assertTrue(!glassSurface.contains("Color.Black.copy"))
    }

    @Test
    fun `lyrics settings expose quarter second offset correction and reset`() {
        val source = loadSource()
        val settings = source
            .substringAfter("private fun LyricsSettingsContent(")
            .substringBefore("private fun LyricLineContent(")

        assertTrue(settings.contains("onLyricsOffsetChange(-250L)"))
        assertTrue(settings.contains("onLyricsOffsetChange(250L)"))
        assertTrue(settings.contains("onLyricsOffsetChange(-lyricsOffsetMs)"))
        assertTrue(settings.contains("formatLyricsOffset(lyricsOffsetMs)"))
    }

    @Test
    fun `glass controls reuse home search backdrop instead of shader background`() {
        val source = loadSource()

        assertTrue(source.contains("MiuixBackdrop?"))
        assertTrue(source.contains("rememberMiuixLayerBackdrop()"))
        assertTrue(source.contains(".miuixLayerBackdrop(musicBackdrop)"))
        assertTrue(source.contains("kernelSuMiuixFloatingDockSurface("))
        assertTrue(!source.contains("liquidGlassBackground("))
    }

    private fun loadSource(
        path: String = "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
    ): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
