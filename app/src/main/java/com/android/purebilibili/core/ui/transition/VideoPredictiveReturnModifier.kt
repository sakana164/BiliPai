package com.android.purebilibili.core.ui.transition

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

internal fun Modifier.videoPredictiveReturnTransform(): Modifier = composed {
    val returnState = LocalVideoPredictiveReturnState.current
    var rootBounds by remember { mutableStateOf<Rect?>(null) }
    val transform = remember(rootBounds, returnState) {
        if (returnState.active) {
            resolveVideoPredictiveReturnTransform(
                rootBounds = rootBounds,
                sourceBounds = returnState.sourceBounds,
                progress = returnState.progress
            )
        } else {
            VideoPredictiveReturnTransform.Identity
        }
    }

    onGloballyPositioned { coordinates ->
        rootBounds = coordinates.boundsInRoot()
    }.graphicsLayer {
        transformOrigin = TransformOrigin.Center
        scaleX = transform.scaleX
        scaleY = transform.scaleY
        translationX = transform.translationX
        translationY = transform.translationY
        alpha = 1f
        clip = transform.cornerRadiusDp > 0.01f
        if (clip) {
            shape = RoundedCornerShape(transform.cornerRadiusDp.dp)
        }
    }
}
