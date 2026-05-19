package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

class PortraitCommentPresentationPolicyTest {

    @Test
    fun `video sub reply expansion should stay inside embedded comment sheet`() {
        assertTrue(shouldUseEmbeddedVideoSubReplyPresentation())
    }

    @Test
    fun `video detail should not mount detached sub reply sheet when embedded path is enabled`() {
        assertFalse(shouldShowDetachedVideoSubReplySheet(useEmbeddedPresentation = true))
    }

    @Test
    fun `video comment reply composer should remain enabled`() {
        assertTrue(shouldOpenPortraitCommentReplyComposer())
    }

    @Test
    fun `video detail should route thread detail inside existing comment sheet when embedded path is enabled`() {
        assertTrue(shouldOpenPortraitCommentThreadDetail(useEmbeddedPresentation = true))
    }

    @Test
    fun `portrait thread detail keeps main sheet presentation even if parent sheet flag is false`() {
        assertTrue(
            resolvePortraitCommentHostMainSheetVisible(
                commentSheetVisible = false,
                subReplyVisible = true
            )
        )
    }

    @Test
    fun `portrait player shrinks while comment sheet is expanded`() {
        assertEquals(0.4f, resolvePortraitCommentExpandedPlayerScale(commentSheetVisible = true), 0.001f)
        assertEquals(1f, resolvePortraitCommentExpandedPlayerScale(commentSheetVisible = false))
        assertTrue(
            abs(
                resolvePortraitCommentExpandedPlayerScale(commentVisibilityProgress = 0.5f) - 0.7f
            ) < 0.001f
        )
    }

    @Test
    fun `portrait comment transform aligns player bottom to sheet top`() {
        val collapsed = resolvePortraitCommentPlayerTransform(
            commentVisibilityProgress = 0f,
            containerHeightPx = 1000
        )
        assertEquals(1f, collapsed.scale, 0.001f)
        assertEquals(0f, collapsed.translationYPx)
        assertEquals(1f, collapsed.visibleHeightFraction, 0.001f)
        assertEquals(1f, collapsed.overlayAlpha, 0.001f)
        assertTrue(collapsed.playerGesturesEnabled)

        val half = resolvePortraitCommentPlayerTransform(
            commentVisibilityProgress = 0.5f,
            containerHeightPx = 1000
        )
        assertEquals(0.7f, half.scale, 0.001f)
        assertEquals(0f, half.translationYPx)
        assertEquals(0.7f, half.visibleHeightFraction, 0.001f)
        assertEquals(0.5f, half.overlayAlpha, 0.001f)
        assertFalse(half.playerGesturesEnabled)

        val expanded = resolvePortraitCommentPlayerTransform(
            commentVisibilityProgress = 1f,
            containerHeightPx = 1000
        )
        assertEquals(0.4f, expanded.scale, 0.001f)
        assertEquals(0f, expanded.translationYPx)
        assertEquals(0.4f, expanded.visibleHeightFraction, 0.001f)
        assertEquals(0f, expanded.overlayAlpha)
        assertFalse(expanded.playerGesturesEnabled)
    }

    @Test
    fun `portrait comment transform clamps unsafe inputs`() {
        assertEquals(
            1f,
            resolvePortraitCommentPlayerTransform(
                commentVisibilityProgress = -1f,
                containerHeightPx = 1000
            ).scale,
            0.001f
        )
        assertEquals(
            0.4f,
            resolvePortraitCommentPlayerTransform(
                commentVisibilityProgress = 2f,
                containerHeightPx = 1000
            ).scale,
            0.001f
        )
        assertEquals(
            1f,
            resolvePortraitCommentPlayerTransform(
                commentVisibilityProgress = 1f,
                containerHeightPx = 0
            ).scale,
            0.001f
        )
    }

    @Test
    fun `comment drag progress follows sheet offset`() {
        assertEquals(1f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 0f, sheetHeightPx = 600f))
        assertEquals(0.5f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 300f, sheetHeightPx = 600f))
        assertEquals(0f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 900f, sheetHeightPx = 600f))
    }

    @Test
    fun `comment drag dismiss triggers after threshold`() {
        assertFalse(
            shouldDismissPortraitCommentSheetByDrag(
                sheetOffsetPx = 100f,
                sheetHeightPx = 600f
            )
        )
        assertTrue(
            shouldDismissPortraitCommentSheetByDrag(
                sheetOffsetPx = 180f,
                sheetHeightPx = 600f
            )
        )
    }

    @Test
    fun `detached thread detail should stay below the player when top area is reserved`() {
        assertEquals(
            0.55f,
            resolveVideoSubReplySheetMaxHeightFraction(
                screenHeightPx = 1000,
                topReservedPx = 450
            )
        )
    }

    @Test
    fun `detached thread detail should stay fullscreen when no top reserve exists`() {
        assertEquals(
            1f,
            resolveVideoSubReplySheetMaxHeightFraction(
                screenHeightPx = 1000,
                topReservedPx = 0
            )
        )
    }
}
