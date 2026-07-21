package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO

internal data class HomeFeedCardLayout(
    val coverAspectRatio: Float,
    val outerPaddingDp: Int,
    val itemSpacingDp: Int,
    val verticalItemSpacingDp: Int = itemSpacingDp,
    val storyCardHorizontalPaddingDp: Int,
    val compactMetadata: Boolean
)

internal fun resolveHomeFeedCardLayout(style: HomeFeedCardStyle): HomeFeedCardLayout {
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HomeFeedCardLayout(
            // 与官方 CDN 封面 16:9 + Crop 一致，避免 4:3 框大幅左右裁切
            coverAspectRatio = VIDEO_SHARED_COVER_ASPECT_RATIO,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            verticalItemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 16,
            compactMetadata = false
        )

        HomeFeedCardStyle.OFFICIAL -> HomeFeedCardLayout(
            // 官方粉版双列：16:9 框 + 居中 Crop（与投稿封面同比例，标准封面几乎完整显示）
            coverAspectRatio = VIDEO_SHARED_COVER_ASPECT_RATIO,
            outerPaddingDp = 4,
            itemSpacingDp = 4,
            verticalItemSpacingDp = 6,
            storyCardHorizontalPaddingDp = 0,
            compactMetadata = true
        )
    }
}
