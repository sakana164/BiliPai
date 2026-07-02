package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardTransitionPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailLoadingTransitionPolicyTest {

    @Test
    fun expandingSharedCardTransitionHidesLoadingSkeletonUntilExpanded() {
        val frame = resolveVideoDetailLoadingTransitionFrame(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDING,
                progress = 0.42f
            )
        )

        assertFalse(frame.showLoadingContent)
        assertEquals(0f, frame.containerBackgroundAlpha)
        assertEquals(0f, frame.loadingContentAlpha)
    }

    @Test
    fun expandingSharedCardTransitionRestoresLoadingSkeletonNearFullscreen() {
        val frame = resolveVideoDetailLoadingTransitionFrame(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDING,
                progress = 0.94f
            )
        )

        assertTrue(frame.showLoadingContent)
        assertEquals(1f, frame.containerBackgroundAlpha)
        assertTrue(frame.loadingContentAlpha in 0f..1f)
        assertTrue(frame.loadingContentAlpha > 0f)
    }

    @Test
    fun expandedOrNonExpandingSessionsShowLoadingSkeletonNormally() {
        val expanded = resolveVideoDetailLoadingTransitionFrame(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDED,
                progress = 1f
            )
        )
        val idle = resolveVideoDetailLoadingTransitionFrame(
            session = VideoCardTransitionSession()
        )
        val collapsing = resolveVideoDetailLoadingTransitionFrame(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.COLLAPSING,
                progress = 0.7f
            )
        )

        assertTrue(expanded.showLoadingContent)
        assertEquals(1f, expanded.containerBackgroundAlpha)
        assertEquals(1f, expanded.loadingContentAlpha)
        assertTrue(idle.showLoadingContent)
        assertTrue(collapsing.showLoadingContent)
    }

    @Test
    fun expandingSharedCardTransitionRevealsSuccessContentProgressivelyNearFullscreen() {
        val early = resolveVideoDetailContainerTransformContentAlpha(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDING,
                progress = 0.8f
            )
        )
        val late = resolveVideoDetailContainerTransformContentAlpha(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDING,
                progress = 0.94f
            )
        )
        val expanded = resolveVideoDetailContainerTransformContentAlpha(
            session = VideoCardTransitionSession(
                phase = VideoCardTransitionPhase.EXPANDED,
                progress = 1f
            )
        )

        assertEquals(0f, early)
        assertTrue(late in 0f..1f)
        assertTrue(late > 0f)
        assertTrue(late < 1f)
        assertEquals(1f, expanded)
    }
}
