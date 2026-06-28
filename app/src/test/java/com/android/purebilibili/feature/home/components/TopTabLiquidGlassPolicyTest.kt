package com.android.purebilibili.feature.home.components

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLiquidGlassPolicyTest {

    @Test
    fun `liquid segmented control replaces bespoke top tab glass renderer`() {
        assertTrue(
            shouldTopTabUseLiquidSegmentedControl(
                isLiquidGlassEnabled = true,
                skinPlainStyle = false,
                hasSkinStickerIcons = false,
                forceMaterialUnderline = false
            )
        )
        assertFalse(
            shouldTopTabUseLiquidSegmentedControl(
                isLiquidGlassEnabled = false,
                skinPlainStyle = false,
                hasSkinStickerIcons = false,
                forceMaterialUnderline = false
            )
        )
        assertFalse(
            shouldTopTabUseLiquidSegmentedControl(
                isLiquidGlassEnabled = true,
                skinPlainStyle = true,
                hasSkinStickerIcons = false,
                forceMaterialUnderline = false
            )
        )
    }

    @Test
    fun `outer dock shell owns container glass and suppresses segmented shell duplication`() {
        assertFalse(
            shouldTopTabDrawSegmentedContainerShell(
                liquidGlassEnabled = true,
                hasOuterChromeSurface = true
            )
        )
        assertFalse(
            shouldTopTabDrawSegmentedCaptureBackdropEffects(
                liquidGlassEnabled = true,
                hasOuterChromeSurface = true
            )
        )
        assertTrue(
            shouldTopTabDrawSegmentedContainerShell(
                liquidGlassEnabled = true,
                hasOuterChromeSurface = false
            )
        )
        assertTrue(
            shouldTopTabDrawSegmentedCaptureBackdropEffects(
                liquidGlassEnabled = true,
                hasOuterChromeSurface = false
            )
        )
    }

    @Test
    fun `pager swipe drives indicator position override while tab drag keeps local state`() {
        assertEquals(
            1.32f,
            resolveTopTabLiquidIndicatorPosition(
                pagerPosition = 1.32f,
                dragPosition = 0f,
                dragActive = false,
                pagerInteractionActive = true
            )!!,
            0.001f
        )
        assertNull(
            resolveTopTabLiquidIndicatorPosition(
                pagerPosition = 1.32f,
                dragPosition = 0.8f,
                dragActive = true,
                pagerInteractionActive = true
            )
        )
        assertNull(
            resolveTopTabLiquidIndicatorPosition(
                pagerPosition = 1.32f,
                dragPosition = 1.32f,
                dragActive = false,
                pagerInteractionActive = false
            )
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }

    @Test
    fun `top tab liquid segmented row reuses bottom bar segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt"
        )
        val liquidBlock = source
            .substringAfter("private fun HomeTopTabLiquidSegmentedTabs(")
            .substringBefore("@Composable\nprivate fun LightweightHomeTopTabs(")

        assertTrue(liquidBlock.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(liquidBlock.contains("drawContainerShell = drawContainerShell"))
        assertTrue(liquidBlock.contains("drawCaptureBackdropEffects = drawCaptureBackdropEffects"))
        assertTrue(liquidBlock.contains("indicatorPositionOverride = indicatorPositionOverride"))
        assertTrue(liquidBlock.contains("resolveMd3TopTabLayoutVisibleSlots("))
        assertTrue(
            source.contains("shouldTopTabUseLiquidSegmentedControl(") &&
                source.contains("HomeTopTabLiquidSegmentedTabs(")
        )
    }

    @Test
    fun `segmented control exposes shell and capture switches for top dock deduplication`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarLiquidSegmentedControl.kt"
        )

        assertTrue(source.contains("drawContainerShell: Boolean = true"))
        assertTrue(source.contains("drawCaptureBackdropEffects: Boolean = true"))
        assertTrue(source.contains("if (drawContainerShell) {"))
        assertTrue(source.contains("drawCaptureBackdropEffects &&"))
    }
}