package com.android.purebilibili.feature.audio.screen

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.android.purebilibili.core.ui.effect.liquidGlassBackground
import com.android.purebilibili.feature.audio.lyrics.LyricLine
import com.android.purebilibili.feature.audio.player.MusicPlayerUiState
import com.android.purebilibili.feature.video.player.PlayMode
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.BackwardEnd
import io.github.alexzhirkevich.cupertino.icons.filled.ForwardEnd
import io.github.alexzhirkevich.cupertino.icons.filled.Pause
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import io.github.alexzhirkevich.cupertino.icons.outlined.Ellipsis
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val MusicFallbackColor = Color(0xFF342B42)
private val MusicContentColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicPlayerContent(
    state: MusicPlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onQueueItemSelected: (Int) -> Unit = {},
    onPlayModeChange: (PlayMode) -> Unit = {},
    onLyricsOffsetChange: (Long) -> Unit = {},
    onLyricsRetry: () -> Unit = {},
    onLyricsSearch: (String) -> Unit = {},
    onLyricsCandidateSelected: (Int) -> Unit = {},
    onVideoModeClick: (() -> Unit)? = null,
    onCollectionClick: (() -> Unit)? = null,
    onSleepTimerClick: (() -> Unit)? = null,
    sleepTimerLabel: String = "定时关闭",
    onPipClick: (() -> Unit)? = null,
    isInPipMode: Boolean = false,
    liquidGlassEffectsEnabled: Boolean = false,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var paletteColor by remember { mutableStateOf(MusicFallbackColor) }
    var artworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyricsSearch by remember { mutableStateOf(false) }
    var lyricSearchText by remember(state.title) { mutableStateOf(state.title) }

    LaunchedEffect(state.coverUrl) {
        val result = loadMusicArtwork(context.imageLoader, state.coverUrl, context)
        artworkBitmap = result?.first
        paletteColor = result?.second ?: MusicFallbackColor
    }

    val backgroundColor by animateColorAsState(
        targetValue = paletteColor,
        animationSpec = tween(if (reduceMotion) 0 else 400),
        label = "music_palette"
    )
    val glassEnabled = resolveMusicLiquidGlassEnabled(
        sdkInt = Build.VERSION.SDK_INT,
        effectsEnabled = liquidGlassEffectsEnabled,
        isAppInBackground = BackgroundManager.isInBackground,
        reduceMotion = reduceMotion
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val layout = resolveMusicPlayerLayout(maxWidth.value.roundToInt(), isInPipMode)
        val availableWidthDp = maxWidth.value.roundToInt()
        val availableHeightDp = maxHeight.value.roundToInt()
        if (layout != MusicPlayerLayout.PIP_ARTWORK) {
            MusicArtworkBackground(
                coverUrl = state.coverUrl,
                backgroundColor = backgroundColor
            )
        }
        when (layout) {
            MusicPlayerLayout.PIP_ARTWORK -> MusicArtwork(
                coverUrl = state.coverUrl,
                bitmap = artworkBitmap,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0
            )

            MusicPlayerLayout.COMPACT_PAGER -> {
                val pagerState = rememberPagerState(pageCount = { 2 })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        PlayerPage(
                            state = state,
                            artworkBitmap = artworkBitmap,
                            artworkSizeDp = resolveMusicArtworkSizeDp(
                                availableWidthDp,
                                availableHeightDp,
                                layout
                            ),
                            glassEnabled = glassEnabled,
                            onPlayPause = onPlayPause,
                            onSeek = onSeek,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onShowQueue = { showQueue = true },
                            onPlayModeChange = onPlayModeChange,
                            onVideoModeClick = onVideoModeClick,
                            onCollectionClick = onCollectionClick,
                            onSleepTimerClick = onSleepTimerClick,
                            sleepTimerLabel = sleepTimerLabel,
                            onPipClick = onPipClick
                        )
                    } else {
                        LyricsPage(
                            state = state,
                            glassEnabled = glassEnabled,
                            onPlayPause = onPlayPause,
                            onSeek = onSeek,
                            onLyricsOffsetChange = onLyricsOffsetChange,
                            onLyricsRetry = onLyricsRetry,
                            onOpenLyricsSearch = { showLyricsSearch = true },
                            reduceMotion = reduceMotion
                        )
                    }
                }
            }

            MusicPlayerLayout.EXPANDED_SPLIT -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 56.dp, start = 32.dp, end = 32.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                PlayerPage(
                    state = state,
                    artworkBitmap = artworkBitmap,
                    artworkSizeDp = resolveMusicArtworkSizeDp(
                        availableWidthDp,
                        availableHeightDp,
                        layout
                    ),
                    glassEnabled = glassEnabled,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onShowQueue = { showQueue = true },
                    onPlayModeChange = onPlayModeChange,
                    onVideoModeClick = onVideoModeClick,
                    onCollectionClick = onCollectionClick,
                    onSleepTimerClick = onSleepTimerClick,
                    sleepTimerLabel = sleepTimerLabel,
                    onPipClick = onPipClick,
                    modifier = Modifier.weight(1f)
                )
                LyricsPage(
                    state = state,
                    glassEnabled = glassEnabled,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onLyricsOffsetChange = onLyricsOffsetChange,
                    onLyricsRetry = onLyricsRetry,
                    onOpenLyricsSearch = { showLyricsSearch = true },
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isInPipMode) {
            MusicTopBar(
                glassEnabled = glassEnabled,
                onBack = onBack,
                onQueue = if (state.queueControls.showQueue) ({ showQueue = true }) else null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = backgroundColor.copy(alpha = 0.96f),
            contentColor = MusicContentColor
        ) {
            Text(
                text = "待播清单",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                itemsIndexed(state.queue, key = { _, item -> item.stableId }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onQueueItemSelected(index)
                                showQueue = false
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (index == state.currentQueueIndex) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = item.artist,
                                color = MusicContentColor.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (index == state.currentQueueIndex) {
                            Icon(CupertinoIcons.Outlined.MusicNote, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (showLyricsSearch) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSearch = false },
            containerColor = backgroundColor.copy(alpha = 0.97f),
            contentColor = MusicContentColor
        ) {
            Text(
                text = "手动匹配歌词",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = lyricSearchText,
                    onValueChange = { lyricSearchText = it },
                    label = { Text("歌名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onLyricsSearch(lyricSearchText) }) {
                    Text("搜索")
                }
            }
            if (state.isLyricsSearching) {
                CircularProgressIndicator(
                    color = MusicContentColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp)
                )
            } else if (state.lyricCandidates.isEmpty()) {
                Text(
                    text = "输入歌名后搜索网易云、QQ 音乐与酷狗",
                    color = MusicContentColor.copy(alpha = 0.62f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    itemsIndexed(state.lyricCandidates) { index, candidate ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLyricsCandidateSelected(index)
                                    showLyricsSearch = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(candidate.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${candidate.artist} · ${candidate.sourceLabel}",
                                color = MusicContentColor.copy(alpha = 0.62f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicArtworkBackground(coverUrl: String, backgroundColor: Color) {
    Box(Modifier.fillMaxSize()) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.52f
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            backgroundColor.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.66f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlayerPage(
    state: MusicPlayerUiState,
    artworkBitmap: ImageBitmap?,
    artworkSizeDp: Int,
    glassEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onShowQueue: () -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,
    onVideoModeClick: (() -> Unit)?,
    onCollectionClick: (() -> Unit)?,
    onSleepTimerClick: (() -> Unit)?,
    sleepTimerLabel: String,
    onPipClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 76.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isLoading && state.coverUrl.isBlank()) {
            CircularProgressIndicator(color = MusicContentColor)
        } else {
            MusicArtwork(
                coverUrl = state.coverUrl,
                bitmap = artworkBitmap,
                modifier = Modifier.size(artworkSizeDp.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = state.title,
                color = MusicContentColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.artist.ifBlank { "未知艺术家" },
                color = MusicContentColor.copy(alpha = 0.64f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            state.error?.let {
                Text(it, color = Color(0xFFFF9B92), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(14.dp))
        MusicProgress(state, onSeek)
        Spacer(Modifier.height(8.dp))
        PlaybackControls(state, onPlayPause, onPrevious, onNext)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayModeButton(state.playMode, glassEnabled, onPlayModeChange)
            if (state.queueControls.showQueue) {
                GlassTextButton("队列", glassEnabled, onShowQueue)
            }
            onVideoModeClick?.let { GlassTextButton("视频", glassEnabled, it) }
            onCollectionClick?.let { GlassTextButton("合集", glassEnabled, it) }
            onSleepTimerClick?.let { GlassTextButton(sleepTimerLabel, glassEnabled, it) }
            onPipClick?.let { GlassTextButton("画中画", glassEnabled, it) }
        }
    }
}

@Composable
private fun MusicArtwork(
    coverUrl: String,
    bitmap: ImageBitmap?,
    modifier: Modifier,
    cornerRadius: Int = 18
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF615571), Color(0xFF27212F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            coverUrl.isNotBlank() -> AsyncImage(
                model = coverUrl,
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            else -> Icon(
                CupertinoIcons.Outlined.MusicNote,
                contentDescription = null,
                tint = MusicContentColor.copy(alpha = 0.78f),
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
private fun MusicProgress(state: MusicPlayerUiState, onSeek: (Long) -> Unit) {
    val duration = state.durationMs.coerceAtLeast(1L)
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    Slider(
        value = draggedPosition ?: state.positionMs.coerceIn(0L, duration).toFloat(),
        onValueChange = { draggedPosition = it },
        onValueChangeFinished = {
            draggedPosition?.let { onSeek(it.toLong()) }
            draggedPosition = null
        },
        valueRange = 0f..duration.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = MusicContentColor,
            activeTrackColor = MusicContentColor,
            inactiveTrackColor = MusicContentColor.copy(alpha = 0.24f)
        )
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatMusicTime(state.positionMs), color = MusicContentColor.copy(alpha = 0.58f), fontSize = 11.sp)
        Text("-${formatMusicTime((state.durationMs - state.positionMs).coerceAtLeast(0L))}", color = MusicContentColor.copy(alpha = 0.58f), fontSize = 11.sp)
    }
}

@Composable
private fun PlaybackControls(
    state: MusicPlayerUiState,
    onPlayPause: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackIconButton(
            icon = CupertinoIcons.Filled.BackwardEnd,
            description = "上一首",
            enabled = state.queueControls.hasPrevious && onPrevious != null,
            onClick = onPrevious ?: {}
        )
        IconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
            if (state.isBuffering) {
                CircularProgressIndicator(color = MusicContentColor, modifier = Modifier.size(36.dp))
            } else {
                Icon(
                    imageVector = if (state.isPlaying) CupertinoIcons.Filled.Pause else CupertinoIcons.Filled.Play,
                    contentDescription = if (state.isPlaying) "暂停" else "播放",
                    tint = MusicContentColor,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        PlaybackIconButton(
            icon = CupertinoIcons.Filled.ForwardEnd,
            description = "下一首",
            enabled = state.queueControls.hasNext && onNext != null,
            onClick = onNext ?: {}
        )
    }
}

@Composable
private fun PlaybackIconButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(56.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MusicContentColor.copy(alpha = if (enabled) 1f else 0.28f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun LyricsPage(
    state: MusicPlayerUiState,
    glassEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onLyricsOffsetChange: (Long) -> Unit,
    onLyricsRetry: () -> Unit,
    onOpenLyricsSearch: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val document = state.lyrics
    val currentIndex = document?.let { resolveCurrentLyricIndex(it, state.positionMs) } ?: -1
    val listState = rememberLazyListState()
    var showTranslations by remember { mutableStateOf(true) }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            if (reduceMotion) {
                listState.scrollToItem(currentIndex, -160)
            } else {
                listState.animateScrollToItem(currentIndex, -160)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 72.dp, bottom = 16.dp)
    ) {
        if (document == null || document.lines.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (state.isLyricsSearching) "正在匹配歌词…" else "暂无歌词",
                    color = MusicContentColor.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextButton("重新匹配", glassEnabled, onLyricsRetry)
                    GlassTextButton("手动搜索", glassEnabled, onOpenLyricsSearch)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 28.dp,
                    top = 120.dp,
                    end = 28.dp,
                    bottom = 160.dp
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                itemsIndexed(document.lines, key = { index, line -> "${line.startTimeMs}:$index" }) { index, line ->
                    LyricLineContent(
                        line = line,
                        isCurrent = index == currentIndex,
                        positionMs = state.positionMs - document.offsetMs,
                        showTranslations = showTranslations,
                        onClick = { onSeek(line.startTimeMs + document.offsetMs) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .musicGlassSurface(glassEnabled, RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onLyricsOffsetChange(-10_000L) }, modifier = Modifier.size(48.dp)) {
                Text("-10", color = MusicContentColor, fontSize = 12.sp)
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(52.dp)) {
                Icon(
                    if (state.isPlaying) CupertinoIcons.Filled.Pause else CupertinoIcons.Filled.Play,
                    contentDescription = if (state.isPlaying) "暂停" else "播放",
                    tint = MusicContentColor
                )
            }
            IconButton(onClick = { onLyricsOffsetChange(10_000L) }, modifier = Modifier.size(48.dp)) {
                Text("+10", color = MusicContentColor, fontSize = 12.sp)
            }
            IconButton(onClick = { showTranslations = !showTranslations }, modifier = Modifier.size(48.dp)) {
                Text(if (showTranslations) "译✓" else "译", color = MusicContentColor, fontSize = 12.sp)
            }
            IconButton(onClick = onOpenLyricsSearch, modifier = Modifier.size(48.dp)) {
                Icon(CupertinoIcons.Outlined.Ellipsis, contentDescription = "搜索歌词", tint = MusicContentColor)
            }
        }
    }
}

@Composable
private fun LyricLineContent(
    line: LyricLine,
    isCurrent: Boolean,
    positionMs: Long,
    showTranslations: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = buildLyricText(line, isCurrent, positionMs),
            color = MusicContentColor.copy(alpha = if (isCurrent) 1f else 0.38f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )
        line.translations.firstOrNull()?.takeIf { showTranslations && it.isNotBlank() }?.let {
            Text(
                text = it,
                color = MusicContentColor.copy(alpha = if (isCurrent) 0.72f else 0.28f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        line.romanization?.takeIf { showTranslations && it.isNotBlank() }?.let {
            Text(
                text = it,
                color = MusicContentColor.copy(alpha = if (isCurrent) 0.58f else 0.24f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

private fun buildLyricText(line: LyricLine, isCurrent: Boolean, positionMs: Long): AnnotatedString {
    if (!isCurrent || line.spans.isEmpty()) return AnnotatedString(line.text)
    return buildAnnotatedString {
        line.spans.forEach { span ->
            val active = positionMs >= span.startTimeMs
            pushStyle(SpanStyle(color = MusicContentColor.copy(alpha = if (active) 1f else 0.38f)))
            append(span.text)
            pop()
        }
    }
}

@Composable
private fun MusicTopBar(
    glassEnabled: Boolean,
    onBack: () -> Unit,
    onQueue: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GlassIconButton(CupertinoIcons.Outlined.ChevronDown, "返回", glassEnabled, onBack)
        onQueue?.let { GlassIconButton(CupertinoIcons.Outlined.Ellipsis, "播放队列", glassEnabled, it) }
            ?: Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    description: String,
    glassEnabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .musicGlassSurface(glassEnabled, CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = MusicContentColor)
    }
}

@Composable
private fun GlassTextButton(label: String, glassEnabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .musicGlassSurface(glassEnabled, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MusicContentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PlayModeButton(mode: PlayMode, glassEnabled: Boolean, onChange: (PlayMode) -> Unit) {
    val next = when (mode) {
        PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
        PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
        PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
    }
    val label = when (mode) {
        PlayMode.SEQUENTIAL -> "顺序"
        PlayMode.SHUFFLE -> "随机"
        PlayMode.REPEAT_ONE -> "单曲"
    }
    GlassTextButton(label, glassEnabled) { onChange(next) }
}

private fun Modifier.musicGlassSurface(
    glassEnabled: Boolean,
    shape: androidx.compose.ui.graphics.Shape
): Modifier {
    val base = clip(shape)
    return if (glassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base
            .liquidGlassBackground(
                refractIntensity = 0.12f,
                scrollOffsetProvider = { 0f },
                backgroundColor = Color.White.copy(alpha = 0.13f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
    } else {
        base
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
    }
}

private suspend fun loadMusicArtwork(
    imageLoader: ImageLoader,
    coverUrl: String,
    context: android.content.Context
): Pair<ImageBitmap, Color>? = withContext(Dispatchers.IO) {
    if (coverUrl.isBlank()) return@withContext null
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
            .size(512, 512)
            .build()
        val result = imageLoader.execute(request) as SuccessResult
        val bitmap = (result.drawable as BitmapDrawable).bitmap
        val palette = Palette.from(bitmap).clearFilters().generate()
        val colorInt = palette.mutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: 0xFF342B42.toInt()
        bitmap.asImageBitmap() to Color(colorInt)
    }.getOrNull()
}

internal fun formatMusicTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
