package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileWallpaperPolicyTest {

    @Test
    fun compactProfileTopBanner_usesHeroFractionAndClamp() {
        val compactHeight = resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact)
        assertEquals(288f, compactHeight, 0.001f)
    }

    @Test
    fun profileTopBannerHeight_staysInsideHeroClampForAllBreakpoints() {
        WindowWidthSizeClass.entries.forEach { sizeClass ->
            val height = resolveProfileTopBannerHeightDp(sizeClass)
            assertTrue(height in 280f..360f)
        }
    }

    @Test
    fun profileImmersiveBackground_isDeferredOnlyDuringBottomPagerTransition() {
        assertEquals(
            false,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = true,
                deferImmersiveRenderBudget = true
            )
        )
        assertEquals(
            true,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = true,
                deferImmersiveRenderBudget = false
            )
        )
        assertEquals(
            false,
            shouldRenderProfileImmersiveBackground(
                hasTopPhoto = false,
                deferImmersiveRenderBudget = false
            )
        )
    }
}
