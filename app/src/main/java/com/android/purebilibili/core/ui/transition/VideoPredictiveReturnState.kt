package com.android.purebilibili.core.ui.transition

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

@Immutable
internal data class VideoPredictiveReturnState(
    val active: Boolean = false,
    val progress: Float = 0f,
    val sourceBounds: Rect? = null
)

internal val LocalVideoPredictiveReturnState = compositionLocalOf {
    VideoPredictiveReturnState()
}

