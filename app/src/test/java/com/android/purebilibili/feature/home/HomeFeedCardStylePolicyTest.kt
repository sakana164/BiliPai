package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedCardStylePolicyTest {

    @Test
    fun currentStyle_usesSixteenByNineAndExistingSpacing() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.CURRENT)

        assertEquals(16f / 9f, layout.coverAspectRatio, 0.0001f)
        assertEquals(8, layout.outerPaddingDp)
        assertEquals(8, layout.itemSpacingDp)
        assertEquals(8, layout.verticalItemSpacingDp)
        assertEquals(16, layout.storyCardHorizontalPaddingDp)
        assertEquals(false, layout.compactMetadata)
    }

    @Test
    fun officialStyle_usesSixteenByNineLikeOfficialClientWithCompactSpacing() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.OFFICIAL)

        // 官方粉版 / CDN 投稿封面 16:9 + Crop，不再用 4:3 大幅左右裁切
        assertEquals(16f / 9f, layout.coverAspectRatio, 0.0001f)
        assertEquals(4, layout.outerPaddingDp)
        assertEquals(4, layout.itemSpacingDp)
        assertEquals(6, layout.verticalItemSpacingDp)
        assertEquals(0, layout.storyCardHorizontalPaddingDp)
        assertEquals(true, layout.compactMetadata)
    }
}
