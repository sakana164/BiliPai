package com.android.purebilibili.feature.audio.lyrics

import java.text.Normalizer
import kotlin.math.abs

internal const val LYRIC_MATCH_MINIMUM_SCORE = 0.72
internal const val LYRIC_MATCH_DURATION_TOLERANCE_MS = 8_000L

internal fun scoreLyricCandidate(
    query: LyricQuery,
    candidate: LyricCandidate
): Double {
    val titleScore = maximumVariantSimilarity(
        lyricTitleMatchVariants(query.title),
        lyricTitleMatchVariants(candidate.title)
    )
    val queryArtistVariants = buildList {
        add(normalizeLyricMatchText(query.artist))
        addAll(extractPerformerVariants(query.title))
    }.filter(String::isNotBlank).distinct()
    val artistScore = maximumVariantSimilarity(
        queryArtistVariants,
        listOf(normalizeLyricMatchText(candidate.artist))
    )
    val durationScore = if (query.durationMs <= 0L || candidate.durationMs <= 0L) {
        1.0
    } else {
        (1.0 - abs(query.durationMs - candidate.durationMs).toDouble() /
            LYRIC_MATCH_DURATION_TOLERANCE_MS.toDouble()).coerceIn(0.0, 1.0)
    }
    return (titleScore * 0.55) + (artistScore * 0.25) + (durationScore * 0.20)
}

private fun lyricTitleMatchVariants(value: String): List<String> {
    val quoted = listOf(
        Regex("《([^》]+)》"),
        Regex("「([^」]+)」"),
        Regex("『([^』]+)』"),
        Regex("[“\"]([^”\"]+)[”\"]")
    ).flatMap { pattern ->
        pattern.findAll(value).map { match ->
            normalizeLyricMatchText(match.groupValues[1], removeBracketedPrefix = true)
        }.toList()
    }
    return (quoted + normalizeLyricMatchText(value, removeBracketedPrefix = true))
        .filter(String::isNotBlank)
        .distinct()
}

private fun extractPerformerVariants(value: String): List<String> {
    return Regex("([\\p{L}\\p{N}· ]{2,24})\\s*[《「『]")
        .findAll(value)
        .map { match -> normalizeLyricMatchText(match.groupValues[1]) }
        .filter(String::isNotBlank)
        .toList()
}

private fun maximumVariantSimilarity(
    leftVariants: List<String>,
    rightVariants: List<String>
): Double {
    return leftVariants.maxOfOrNull { left ->
        rightVariants.maxOfOrNull { right -> stringSimilarity(left, right) } ?: 0.0
    } ?: 0.0
}

internal fun selectBestLyricCandidate(
    query: LyricQuery,
    candidates: List<LyricCandidate>,
    minimumScore: Double = LYRIC_MATCH_MINIMUM_SCORE,
    durationToleranceMs: Long = LYRIC_MATCH_DURATION_TOLERANCE_MS
): LyricCandidate? {
    return candidates
        .asSequence()
        .filter { candidate ->
            query.durationMs <= 0L || candidate.durationMs <= 0L ||
                abs(query.durationMs - candidate.durationMs) <= durationToleranceMs
        }
        .map { it to scoreLyricCandidate(query, it) }
        .filter { (_, score) -> score >= minimumScore }
        .sortedWith(
            compareByDescending<Pair<LyricCandidate, Double>> { it.second }
                .thenBy { providerPriority(it.first.source) }
        )
        .firstOrNull()
        ?.first
}

private fun providerPriority(source: LyricSource): Int = when (source) {
    LyricSource.NETEASE -> 0
    LyricSource.QQ_MUSIC -> 1
    LyricSource.KUGOU -> 2
    LyricSource.BILIBILI -> 3
    LyricSource.MANUAL -> 4
}

private fun normalizeLyricMatchText(
    value: String,
    removeBracketedPrefix: Boolean = false
): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase()
    val withoutPrefix = if (removeBracketedPrefix) {
        normalized.replace(Regex("^(?:【[^】]*】|\\[[^]]*])+"), "")
    } else {
        normalized
    }
    return withoutPrefix.replace(Regex("[^\\p{L}\\p{N}]"), "")
}

private fun stringSimilarity(left: String, right: String): Double {
    if (left == right) return 1.0
    if (left.isEmpty() || right.isEmpty()) return 0.0
    val previous = IntArray(right.length + 1)
    val current = IntArray(right.length + 1)
    left.forEach { leftChar ->
        for (index in right.indices) {
            current[index + 1] = if (leftChar == right[index]) {
                previous[index] + 1
            } else {
                maxOf(previous[index + 1], current[index])
            }
        }
        current.copyInto(previous)
        current.fill(0)
    }
    return previous[right.length].toDouble() / maxOf(left.length, right.length).toDouble()
}
