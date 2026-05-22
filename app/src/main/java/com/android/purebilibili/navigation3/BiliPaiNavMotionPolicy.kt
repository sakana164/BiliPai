package com.android.purebilibili.navigation3

import com.android.purebilibili.core.store.PredictiveBackAnimationStyle
import com.android.purebilibili.navigation.AppSystemBackAction

internal enum class BiliPaiNavMotionMode {
    CARD_DISABLED,
    CLASSIC_CARD,
    PREDICTIVE_NAV_DISPLAY
}

internal enum class BiliPaiNavRouteTransition {
    NO_OP_SHARED_ELEMENT,
    HOME_VIDEO_SHEET_FORWARD,
    HOME_VIDEO_SHEET_RETURN,
    CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT,
    CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT,
    CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
    CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
    NAV_DISPLAY_DEFAULT_PREDICTIVE,
    CLASSIC_CARD,
    FALLBACK
}

internal data class BiliPaiNavMotionDecision(
    val mode: BiliPaiNavMotionMode,
    val routeTransition: BiliPaiNavRouteTransition,
    val interceptSystemBack: Boolean
)

internal enum class BiliPaiBackGestureOwner {
    NAV_DISPLAY_PREDICTIVE,
    APP_CLASSIC,
    APP_ACTION
}

internal data class BiliPaiBackGestureDecision(
    val owner: BiliPaiBackGestureOwner,
    val routeTransition: BiliPaiNavRouteTransition
) {
    val interceptSystemBack: Boolean
        get() = owner != BiliPaiBackGestureOwner.NAV_DISPLAY_PREDICTIVE
}

internal fun resolveBiliPaiNavMotionMode(
    predictiveBackAnimationStyle: PredictiveBackAnimationStyle,
    cardTransitionEnabled: Boolean
): BiliPaiNavMotionMode {
    return if (predictiveBackAnimationStyle.usesPredictiveBack) {
        BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY
    } else if (cardTransitionEnabled) {
        BiliPaiNavMotionMode.CLASSIC_CARD
    } else {
        BiliPaiNavMotionMode.CARD_DISABLED
    }
}

internal fun resolveBiliPaiNavMotionDecision(
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    predictiveBackAnimationStyle: PredictiveBackAnimationStyle,
    cardTransitionEnabled: Boolean,
    sharedTransitionReady: Boolean,
    appBackActionRequiresInterception: Boolean = false
): BiliPaiNavMotionDecision {
    val mode = resolveBiliPaiNavMotionMode(
        predictiveBackAnimationStyle = predictiveBackAnimationStyle,
        cardTransitionEnabled = cardTransitionEnabled
    )
    val isVideoToCardReturn = fromKey is BiliPaiNavKey.VideoDetail &&
        toKey != null &&
        isCardReturnTargetNavKey(toKey)
    val isCardToVideoForward = fromKey != null &&
        isCardReturnTargetNavKey(fromKey) &&
        toKey is BiliPaiNavKey.VideoDetail
    val routeTransition = when {
        cardTransitionEnabled &&
            sharedTransitionReady &&
            (isVideoToCardReturn || isCardToVideoForward) ->
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
        mode == BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY ->
            BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE
        mode == BiliPaiNavMotionMode.CLASSIC_CARD ->
            BiliPaiNavRouteTransition.CLASSIC_CARD
        else -> BiliPaiNavRouteTransition.FALLBACK
    }

    return BiliPaiNavMotionDecision(
        mode = mode,
        routeTransition = routeTransition,
        interceptSystemBack = shouldInterceptSystemBackForNavigation3(
            mode = mode,
            appBackActionRequiresInterception = appBackActionRequiresInterception
        )
    )
}

internal fun resolveBiliPaiBackGestureDecision(
    predictiveBackAnimationStyle: PredictiveBackAnimationStyle,
    cardTransitionEnabled: Boolean,
    systemBackAction: AppSystemBackAction,
    currentKey: BiliPaiNavKey?,
    previousKey: BiliPaiNavKey?,
    sourceMetadata: BiliPaiNavSourceMetadata
): BiliPaiBackGestureDecision {
    val motionMode = resolveBiliPaiNavMotionMode(
        predictiveBackAnimationStyle = predictiveBackAnimationStyle,
        cardTransitionEnabled = cardTransitionEnabled
    )
    val routeTransition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
        motionMode = motionMode,
        cardTransitionEnabled = cardTransitionEnabled,
        sourceMetadata = sourceMetadata,
        fromKey = currentKey,
        toKey = previousKey
    )
    val shouldUseClassicVideoSharedReturn = currentKey is BiliPaiNavKey.VideoDetail &&
        previousKey != null &&
        isCardReturnTargetNavKey(previousKey) &&
        routeTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
    val owner = when (systemBackAction) {
        AppSystemBackAction.RETURN_TO_HOME_TAB -> BiliPaiBackGestureOwner.APP_ACTION
        AppSystemBackAction.NAVIGATE_UP -> {
            if (shouldUseClassicVideoSharedReturn) {
                // 视频共享元素回程必须先由应用壳标记 returning，再 pop；
                // 否则 NavDisplay 预测性返回会早一帧移除详情页，导致首页闪屏。
                BiliPaiBackGestureOwner.APP_CLASSIC
            } else if (predictiveBackAnimationStyle.usesPredictiveBack) {
                BiliPaiBackGestureOwner.NAV_DISPLAY_PREDICTIVE
            } else {
                BiliPaiBackGestureOwner.APP_CLASSIC
            }
        }
        AppSystemBackAction.FINISH_ACTIVITY -> {
            if (predictiveBackAnimationStyle.usesPredictiveBack) {
                BiliPaiBackGestureOwner.NAV_DISPLAY_PREDICTIVE
            } else {
                BiliPaiBackGestureOwner.APP_CLASSIC
            }
        }
    }
    return BiliPaiBackGestureDecision(
        owner = owner,
        routeTransition = if (owner == BiliPaiBackGestureOwner.APP_ACTION) {
            BiliPaiNavRouteTransition.FALLBACK
        } else {
            routeTransition
        }
    )
}

internal fun resolveBiliPaiNavDisplayPredictivePopRouteTransition(
    motionMode: BiliPaiNavMotionMode,
    cardTransitionEnabled: Boolean = true,
    sourceMetadata: BiliPaiNavSourceMetadata,
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?
): BiliPaiNavRouteTransition {
    val fromVideoKey = fromKey as? BiliPaiNavKey.VideoDetail
    val normalizedSourceRoute = sourceMetadata.sourceRoute?.substringBefore("?")
    val normalizedVideoRoute = fromVideoKey?.sourceRoute?.substringBefore("?")
    val sourceMatchesCurrentVideo = fromVideoKey != null &&
        normalizedSourceRoute != null &&
        normalizedVideoRoute == normalizedSourceRoute &&
        sourceMetadata.sourceKey == "$normalizedSourceRoute:${fromVideoKey.bvid}"
    val sharedReadyVideoToSourceCard = sourceMetadata.sharedTransitionReady &&
        sourceMatchesCurrentVideo &&
        toKey != null &&
        isCardReturnTargetNavKey(toKey)
    if (cardTransitionEnabled && sharedReadyVideoToSourceCard) {
        return BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
    }
    if (!cardTransitionEnabled && sharedReadyVideoToSourceCard) {
        resolveCardDisabledPredictiveReturnTransition(sourceMetadata.cardSourceDirection)?.let {
            return it
        }
    }
    return if (shouldUseNavigation3PredictivePop(motionMode)) {
        BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE
    } else {
        BiliPaiNavRouteTransition.CLASSIC_CARD
    }
}

internal fun shouldInterceptSystemBackForNavigation3(
    mode: BiliPaiNavMotionMode,
    appBackActionRequiresInterception: Boolean
): Boolean {
    if (appBackActionRequiresInterception) return true
    return mode == BiliPaiNavMotionMode.CLASSIC_CARD
}

internal fun shouldUseNavigation3PredictivePop(mode: BiliPaiNavMotionMode): Boolean {
    return mode == BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY
}

internal fun shouldSuppressPredictiveBackDecoratorForRouteTransition(
    routeTransition: BiliPaiNavRouteTransition
): Boolean {
    return when (routeTransition) {
        BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
        BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT -> true
        else -> false
    }
}

private fun resolveCardDisabledPredictiveReturnTransition(
    sourceDirection: BiliPaiNavCardSourceDirection
): BiliPaiNavRouteTransition? {
    return when (sourceDirection) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT ->
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
        BiliPaiNavCardSourceDirection.NONE -> null
    }
}
