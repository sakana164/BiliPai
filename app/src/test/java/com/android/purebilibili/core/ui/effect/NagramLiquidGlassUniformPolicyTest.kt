package com.android.purebilibili.core.ui.effect

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NagramLiquidGlassUniformPolicyTest {

    @Test
    fun `missing previous state always updates uniforms`() {
        assertTrue(
            shouldUpdateNagramLiquidGlassUniforms(
                previous = null,
                next = baseState()
            )
        )
    }

    @Test
    fun `small float changes within nagramx threshold do not update uniforms`() {
        val previous = baseState()
        val next = previous.copy(
            centerX = previous.centerX + 0.05f,
            capsuleCenterX = previous.capsuleCenterX - 0.05f
        )

        assertFalse(shouldUpdateNagramLiquidGlassUniforms(previous, next))
    }

    @Test
    fun `float changes above nagramx threshold update uniforms`() {
        val previous = baseState()
        val next = previous.copy(
            centerX = previous.centerX + 0.1001f
        )

        assertTrue(shouldUpdateNagramLiquidGlassUniforms(previous, next))
    }

    @Test
    fun `color and capsule enable changes update uniforms`() {
        val previous = baseState()

        assertTrue(
            shouldUpdateNagramLiquidGlassUniforms(
                previous = previous,
                next = previous.copy(foregroundColorArgb = 0x22FFFFFF)
            )
        )
        assertTrue(
            shouldUpdateNagramLiquidGlassUniforms(
                previous = previous,
                next = previous.copy(capsuleEnabled = false)
            )
        )
    }

    private fun baseState(): NagramLiquidGlassUniformState {
        return NagramLiquidGlassUniformState(
            width = 320f,
            height = 88f,
            centerX = 160f,
            centerY = 44f,
            sizeX = 160f,
            sizeY = 44f,
            radius = 28f,
            thickness = 11f,
            refractIndex = 1.5f,
            refractIntensity = 0.75f,
            foregroundColorArgb = 0x00FFFFFF,
            capsuleEnabled = true,
            capsuleCenterX = 160f,
            capsuleCenterY = 44f,
            capsuleSizeX = 72f,
            capsuleSizeY = 28f,
            capsuleRadius = 28f,
            capsuleThickness = 11f,
            capsuleRefractIntensity = 0.8f,
            capsuleForegroundColorArgb = 0x00FFFFFF
        )
    }
}
