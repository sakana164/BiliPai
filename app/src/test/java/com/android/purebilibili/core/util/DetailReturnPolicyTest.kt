package com.android.purebilibili.core.util

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailReturnPolicyTest {

    @Test
    fun quickReturn_whenBackWithinThreshold_returnsTrue() {
        assertTrue(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_420L
            )
        )
    }

    @Test
    fun slowReturn_whenBackAfterThreshold_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 1_000L,
                detailReturnUptimeMs = 1_650L
            )
        )
    }

    @Test
    fun invalidTimeline_returnsFalse() {
        assertFalse(
            shouldUseQuickReturnSharedTransitionPolicy(
                detailEnterUptimeMs = 2_000L,
                detailReturnUptimeMs = 1_900L
            )
        )
    }

    @Test
    fun clear_resetsOnlyCardGeometryFallback() {
        CardPositionManager.recordCardPosition(
            bounds = Rect(0f, 0f, 100f, 100f),
            screenWidth = 200f,
            screenHeight = 200f
        )
        assertTrue(CardPositionManager.lastClickedCardBounds != null)
        assertTrue(CardPositionManager.lastClickedCardCenter != null)

        CardPositionManager.clear()

        assertNull(CardPositionManager.lastClickedCardBounds)
        assertNull(CardPositionManager.lastClickedCardCenter)
    }

    @Test
    fun visibleCardBelowTopChromeAllowsSharedContainerTransition() {
        CardPositionManager.recordVideoCardPosition(
            bvid = "BV1",
            sourceRoute = "home",
            bounds = Rect(left = 20f, top = 200f, right = 220f, bottom = 420f),
            screenWidth = 1080f,
            screenHeight = 2400f,
            density = 3f
        )

        assertTrue(CardPositionManager.isCardFullyVisible)
    }

    @Test
    fun mostlyOffscreenCardDisablesSharedContainerTransition() {
        CardPositionManager.recordVideoCardPosition(
            bvid = "BV1",
            sourceRoute = "home",
            bounds = Rect(left = 20f, top = -190f, right = 220f, bottom = 20f),
            screenWidth = 1080f,
            screenHeight = 2400f,
            density = 3f
        )

        assertFalse(CardPositionManager.isCardFullyVisible)
    }
}
