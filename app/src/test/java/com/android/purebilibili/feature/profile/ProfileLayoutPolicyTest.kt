package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileLayoutPolicyTest {

    @Test
    fun heroHeight_usesScreenFractionAndClamps() {
        val tokens = resolveProfileLayoutTokens()
        val heroHeight = resolveProfileHeroHeightDp(screenHeightDp = 800)

        assertEquals(800 * tokens.heroHeightFraction, heroHeight, 0.001f)
        assertTrue(heroHeight in tokens.heroMinHeightDp.toFloat()..tokens.heroMaxHeightDp.toFloat())
    }

    @Test
    fun heroHeight_clampsOnVerySmallScreens() {
        assertEquals(280f, resolveProfileHeroHeightDp(screenHeightDp = 500))
    }

    @Test
    fun heroHeight_clampsOnVeryTallScreens() {
        assertEquals(360f, resolveProfileHeroHeightDp(screenHeightDp = 1200))
    }

    @Test
    fun topBannerHeight_delegatesToHeroHeightPolicy() {
        assertEquals(
            resolveProfileHeroHeightDp(
                screenHeightDp = referenceProfileScreenHeightDp(WindowWidthSizeClass.Compact),
                widthSizeClass = WindowWidthSizeClass.Compact
            ),
            resolveProfileTopBannerHeightDp(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun cardTokens_useUnifiedPosterDimensions() {
        val tokens = resolveProfileCardTokens()

        assertEquals(140, tokens.widthDp)
        assertEquals(12, tokens.cornerRadiusDp)
        assertEquals(52, tokens.metadataHeightDp)
        assertTrue(resolveProfileCardHeightDp(tokens) > resolveProfileCardCoverHeightDp(tokens))
    }
}