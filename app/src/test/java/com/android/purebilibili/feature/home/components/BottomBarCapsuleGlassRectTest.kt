package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarCapsuleGlassRectTest {

    @Test
    fun `capsule is centered on the indicator x and vertically in the layer`() {
        val rect = resolveBottomBarCapsuleGlassRect(
            indicatorCenterXPx = 240f,
            layerHeightPx = 160f,
            indicatorWidthPx = 180f,
            indicatorHeightPx = 112f,
            thicknessPx = 11f,
            refractIntensity = 0.6f
        )

        assertEquals(240f, rect.center.x)
        assertEquals(80f, rect.center.y)
    }

    @Test
    fun `capsule size matches the indicator size`() {
        val rect = resolveBottomBarCapsuleGlassRect(
            indicatorCenterXPx = 100f,
            layerHeightPx = 160f,
            indicatorWidthPx = 180f,
            indicatorHeightPx = 112f,
            thicknessPx = 11f,
            refractIntensity = 0.6f
        )

        assertEquals(180f, rect.size.width)
        assertEquals(112f, rect.size.height)
    }

    @Test
    fun `corner radius is half the indicator height so the capsule is a pill`() {
        val rect = resolveBottomBarCapsuleGlassRect(
            indicatorCenterXPx = 100f,
            layerHeightPx = 160f,
            indicatorWidthPx = 180f,
            indicatorHeightPx = 112f,
            thicknessPx = 11f,
            refractIntensity = 0.6f
        )

        assertEquals(56f, rect.cornerRadius)
    }

    @Test
    fun `thickness and refract intensity pass through unchanged`() {
        val rect = resolveBottomBarCapsuleGlassRect(
            indicatorCenterXPx = 100f,
            layerHeightPx = 160f,
            indicatorWidthPx = 180f,
            indicatorHeightPx = 112f,
            thicknessPx = 9f,
            refractIntensity = 0.42f
        )

        assertEquals(9f, rect.thickness)
        assertEquals(0.42f, rect.refractIntensity)
    }
}
