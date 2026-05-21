package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPredictiveReturnPolicyTest {

    @Test
    fun zeroProgress_keepsDetailPageUnchanged() {
        val transform = resolveVideoPredictiveReturnTransform(
            rootBounds = Rect(0f, 0f, 1080f, 2400f),
            sourceBounds = Rect(120f, 420f, 520f, 720f),
            progress = 0f
        )

        assertEquals(1f, transform.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(1f, transform.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(0f, transform.translationX, absoluteTolerance = 0.0001f)
        assertEquals(0f, transform.translationY, absoluteTolerance = 0.0001f)
        assertEquals(0f, transform.cornerRadiusDp, absoluteTolerance = 0.0001f)
    }

    @Test
    fun fullProgress_mapsDetailPageToSourceCardBounds() {
        val transform = resolveVideoPredictiveReturnTransform(
            rootBounds = Rect(0f, 0f, 1080f, 2400f),
            sourceBounds = Rect(120f, 420f, 520f, 720f),
            progress = 1f,
            targetCornerRadiusDp = 14f
        )

        assertEquals(400f / 1080f, transform.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(300f / 2400f, transform.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(-220f, transform.translationX, absoluteTolerance = 0.0001f)
        assertEquals(-630f, transform.translationY, absoluteTolerance = 0.0001f)
        assertEquals(14f, transform.cornerRadiusDp, absoluteTolerance = 0.0001f)
    }

    @Test
    fun invalidBounds_fallsBackToIdentity() {
        val transform = resolveVideoPredictiveReturnTransform(
            rootBounds = Rect(0f, 0f, 0f, 2400f),
            sourceBounds = Rect(120f, 420f, 520f, 720f),
            progress = 1f
        )

        assertEquals(VideoPredictiveReturnTransform.Identity, transform)
    }
}
