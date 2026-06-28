package com.android.purebilibili.feature.profile

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileChromePolicyTest {

    @Test
    fun wallpaperHero_usesWhiteTextAndStrongerDarkThemeScrim() {
        val lightHero = resolveProfileHeroChrome(
            hasWallpaper = true,
            isDarkTheme = false,
            onSurfaceColor = Color.Black,
            onSurfaceVariantColor = Color.Gray
        )
        val darkHero = resolveProfileHeroChrome(
            hasWallpaper = true,
            isDarkTheme = true,
            onSurfaceColor = Color.White,
            onSurfaceVariantColor = Color.Gray
        )

        assertEquals(Color.White, lightHero.textColor)
        assertEquals(0.55f, lightHero.scrimBottomAlpha)
        assertEquals(0.65f, darkHero.scrimBottomAlpha)
        assertFalse(lightHero.useLightStatusBarIcons)
    }

    @Test
    fun fallbackHero_usesThemeTextAndLightStatusBarOnLightTheme() {
        val hero = resolveProfileHeroChrome(
            hasWallpaper = false,
            isDarkTheme = false,
            onSurfaceColor = Color.Black,
            onSurfaceVariantColor = Color.DarkGray
        )

        assertEquals(Color.Black, hero.textColor)
        assertTrue(hero.useLightStatusBarIcons)
        assertEquals(0f, hero.scrimBottomAlpha)
    }

    @Test
    fun contentChrome_usesOpaqueSurfaceAndThemeElevation() {
        val lightChrome = resolveProfileContentChrome(
            surfaceColor = Color.White,
            onSurfaceColor = Color.Black,
            onSurfaceVariantColor = Color.Gray,
            primaryColor = Color.Blue,
            surfaceContainerHighColor = Color(0xFFECECEC),
            isDarkTheme = false
        )
        val darkChrome = resolveProfileContentChrome(
            surfaceColor = Color(0xFF121212),
            onSurfaceColor = Color.White,
            onSurfaceVariantColor = Color.LightGray,
            primaryColor = Color.Cyan,
            surfaceContainerHighColor = Color(0xFF1E1E1E),
            isDarkTheme = true
        )

        assertEquals(Color.White, lightChrome.surfaceColor)
        assertEquals(2, lightChrome.sheetShadowElevationDp)
        assertEquals(0, darkChrome.sheetShadowElevationDp)
        assertTrue(resolveProfileContentUsesOpaqueSurface(hasWallpaper = true))
    }

    @Test
    fun fallbackGradient_onlyAppliesWithoutWallpaper() {
        assertNull(
            resolveProfileHeroFallbackGradient(
                hasWallpaper = true,
                isDarkTheme = false,
                surfaceColor = Color.White,
                surfaceVariantColor = Color.LightGray,
                primaryContainerColor = Color.Blue
            )
        )
        val gradient = resolveProfileHeroFallbackGradient(
            hasWallpaper = false,
            isDarkTheme = true,
            surfaceColor = Color.Black,
            surfaceVariantColor = Color.DarkGray,
            primaryContainerColor = Color.Blue
        )
        assertEquals(Color.DarkGray.copy(alpha = 0.92f), gradient?.topColor)
        assertEquals(Color.Black, gradient?.bottomColor)
    }
}