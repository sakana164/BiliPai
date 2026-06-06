package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImagePreviewDecodePolicyTest {

    @Test
    fun fullscreenPreview_staysBelowCanvasBitmapLimit() {
        val size = resolveImageDecodeSize(ImageDecodeTarget.FULLSCREEN_PREVIEW)

        assertEquals(ImageDecodeSize(widthPx = 4096, heightPx = 4096), size)
        assertTrue(estimateArgb8888ByteCount(size) < 100L * 1024L * 1024L)
    }

    @Test
    fun commentThumbnail_usesSmallerDecodeBudget() {
        val previewSize = resolveImageDecodeSize(ImageDecodeTarget.FULLSCREEN_PREVIEW)
        val thumbnailSize = resolveImageDecodeSize(ImageDecodeTarget.COMMENT_THUMBNAIL)

        assertEquals(ImageDecodeSize(widthPx = 1024, heightPx = 1024), thumbnailSize)
        assertTrue(estimateArgb8888ByteCount(thumbnailSize) < estimateArgb8888ByteCount(previewSize))
    }
}
