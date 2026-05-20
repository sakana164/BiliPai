package com.android.purebilibili.navigation3

internal data class BiliPaiNavSourceMetadata(
    val sourceKey: String? = null,
    val sourceRoute: String? = null,
    val clickedBoundsRecorded: Boolean = false,
    val cardFullyVisible: Boolean = false
) {
    val sharedTransitionReady: Boolean
        get() = clickedBoundsRecorded && cardFullyVisible
}

internal fun resolveBiliPaiNavSourceMetadata(
    sourceKey: String? = null,
    sourceRoute: String? = null,
    clickedBoundsRecorded: Boolean,
    cardFullyVisible: Boolean
): BiliPaiNavSourceMetadata {
    return BiliPaiNavSourceMetadata(
        sourceKey = sourceKey,
        sourceRoute = sourceRoute?.substringBefore("?"),
        clickedBoundsRecorded = clickedBoundsRecorded,
        cardFullyVisible = cardFullyVisible
    )
}
