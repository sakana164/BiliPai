package com.android.purebilibili.navigation3

import com.android.purebilibili.core.store.PredictiveBackAnimationStyle
import com.android.purebilibili.navigation.AppSystemBackAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiliPaiNavMotionPolicyTest {

    @Test
    fun predictiveEnabledWithCards_usesNavDisplayPredictiveMode() {
        assertEquals(
            BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
            resolveBiliPaiNavMotionMode(
                predictiveBackAnimationStyle = PredictiveBackAnimationStyle.AOSP,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun predictiveDisabledWithCards_usesClassicCardMode() {
        assertEquals(
            BiliPaiNavMotionMode.CLASSIC_CARD,
            resolveBiliPaiNavMotionMode(
                predictiveBackAnimationStyle = PredictiveBackAnimationStyle.NONE,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun installerXStylesWithCards_useNavDisplayPredictiveMode() {
        listOf(
            PredictiveBackAnimationStyle.AOSP,
            PredictiveBackAnimationStyle.MIUIX,
            PredictiveBackAnimationStyle.SCALE,
            PredictiveBackAnimationStyle.CLASSIC
        ).forEach { style ->
            assertEquals(
                BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
                resolveBiliPaiNavMotionMode(
                    predictiveBackAnimationStyle = style,
                    cardTransitionEnabled = true
                )
            )
        }
    }

    @Test
    fun installerXPredictiveStyles_doNotDependOnCardTransitionSwitch() {
        listOf(
            PredictiveBackAnimationStyle.AOSP,
            PredictiveBackAnimationStyle.MIUIX,
            PredictiveBackAnimationStyle.SCALE,
            PredictiveBackAnimationStyle.CLASSIC
        ).forEach { style ->
            assertEquals(
                BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
                resolveBiliPaiNavMotionMode(
                    predictiveBackAnimationStyle = style,
                    cardTransitionEnabled = false
                )
            )
        }
    }

    @Test
    fun sharedElementReady_videoReturn_prefersNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.AOSP,
            cardTransitionEnabled = true,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertFalse(decision.interceptSystemBack)
    }

    @Test
    fun disabledCardTransitionWithSharedReadyVideoReturnDoesNotUseNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.NONE,
            cardTransitionEnabled = false,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavMotionMode.CARD_DISABLED, decision.mode)
        assertEquals(BiliPaiNavRouteTransition.FALLBACK, decision.routeTransition)
        assertFalse(decision.interceptSystemBack)
    }

    @Test
    fun predictiveEnabledSharedVideoReturnUsesClassicAppBack() {
        val decision = resolveBiliPaiBackGestureDecision(
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.AOSP,
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.NAVIGATE_UP,
            currentKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            previousKey = BiliPaiNavKey.Home,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiBackGestureOwner.APP_CLASSIC, decision.owner)
        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun predictiveEnabledStaleVideoReturnUsesNavDisplayDefaultPredictivePop() {
        val decision = resolveBiliPaiBackGestureDecision(
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.AOSP,
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.NAVIGATE_UP,
            currentKey = BiliPaiNavKey.VideoDetail("BV2", sourceRoute = "home"),
            previousKey = BiliPaiNavKey.Home,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiBackGestureOwner.NAV_DISPLAY_PREDICTIVE, decision.owner)
        assertEquals(BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE, decision.routeTransition)
        assertFalse(decision.interceptSystemBack)
    }

    @Test
    fun predictiveDisabledNavigateUpUsesClassicAppBack() {
        val decision = resolveBiliPaiBackGestureDecision(
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.NONE,
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.NAVIGATE_UP,
            currentKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            previousKey = BiliPaiNavKey.Home,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiBackGestureOwner.APP_CLASSIC, decision.owner)
        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun returnToHomeTabAlwaysUsesAppActionBack() {
        val decision = resolveBiliPaiBackGestureDecision(
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.AOSP,
            cardTransitionEnabled = true,
            systemBackAction = AppSystemBackAction.RETURN_TO_HOME_TAB,
            currentKey = BiliPaiNavKey.MainHost,
            previousKey = null,
            sourceMetadata = BiliPaiNavSourceMetadata()
        )

        assertEquals(BiliPaiBackGestureOwner.APP_ACTION, decision.owner)
        assertEquals(BiliPaiNavRouteTransition.FALLBACK, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun navDisplayPredictivePop_sharedReadyVideoReturn_keepsRouteLayerNoOp() {
        val transition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
            cardTransitionEnabled = true,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "history:BV1",
                sourceRoute = "history",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "history"),
            toKey = BiliPaiNavKey.History
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, transition)
    }

    @Test
    fun navDisplayPredictivePop_disabledSharedTransition_usesDirectionalReturnFallback() {
        val transition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            toKey = BiliPaiNavKey.Home
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT, transition)
    }

    @Test
    fun navDisplayPop_disabledSharedTransition_supportsHistoryAndFavoriteCardSources() {
        val historyTransition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.CARD_DISABLED,
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "history:BV1",
                sourceRoute = "history",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_LEFT
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "history"),
            toKey = BiliPaiNavKey.History
        )
        val favoriteTransition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.CARD_DISABLED,
            cardTransitionEnabled = false,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "favorite:BV2",
                sourceRoute = "favorite",
                clickedBoundsRecorded = true,
                cardFullyVisible = true,
                cardSourceDirection = BiliPaiNavCardSourceDirection.SOURCE_RIGHT
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV2", sourceRoute = "favorite"),
            toKey = BiliPaiNavKey.Favorite
        )

        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT, historyTransition)
        assertEquals(BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT, favoriteTransition)
    }

    @Test
    fun directionalReturnFallbackSuppressesPredictiveDecorator() {
        assertTrue(
            shouldSuppressPredictiveBackDecoratorForRouteTransition(
                BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT
            )
        )
        assertTrue(
            shouldSuppressPredictiveBackDecoratorForRouteTransition(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
            )
        )
        assertTrue(
            shouldSuppressPredictiveBackDecoratorForRouteTransition(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
            )
        )
        assertFalse(
            shouldSuppressPredictiveBackDecoratorForRouteTransition(
                BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE
            )
        )
    }

    @Test
    fun plainPopOverridesOnlySharedOrDirectionalVideoReturnTransitions() {
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT)
        )
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT
            )
        )
        assertNotNull(
            resolveBiliPaiNavPopContentTransform(
                BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT
            )
        )
        assertNull(
            resolveBiliPaiNavPopContentTransform(BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE)
        )
        assertNull(
            resolveBiliPaiNavPopContentTransform(BiliPaiNavRouteTransition.FALLBACK)
        )
    }

    @Test
    fun entryPop_videoReturnToRecordedSource_keepsRouteLayerNoOp() {
        val transition = resolveBiliPaiNavEntryPopRouteTransition(
            defaultTransition = BiliPaiNavRouteTransition.FALLBACK,
            fromRoute = "video",
            toRoute = "home",
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, transition)
    }

    @Test
    fun entryPop_videoReturnToDifferentSource_usesFallbackRouteLayer() {
        val transition = resolveBiliPaiNavEntryPopRouteTransition(
            defaultTransition = BiliPaiNavRouteTransition.FALLBACK,
            fromRoute = "video",
            toRoute = "dynamic",
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.FALLBACK, transition)
    }

    @Test
    fun entryPop_dynamicDetailReturnWithStaleVideoMetadata_usesFallbackRouteLayer() {
        val transition = resolveBiliPaiNavEntryPopRouteTransition(
            defaultTransition = BiliPaiNavRouteTransition.FALLBACK,
            fromRoute = "dynamic_detail",
            toRoute = "dynamic",
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            )
        )

        assertEquals(BiliPaiNavRouteTransition.FALLBACK, transition)
    }

    @Test
    fun navDisplayPredictivePop_withoutSharedReady_usesNavDisplayDefaultPredictivePop() {
        val transition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
            cardTransitionEnabled = true,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "history:BV1",
                sourceRoute = "history",
                clickedBoundsRecorded = true,
                cardFullyVisible = false
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "history"),
            toKey = BiliPaiNavKey.History
        )

        assertEquals(BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE, transition)
    }

    @Test
    fun navDisplayPredictivePop_withStaleVideoSource_usesNavDisplayDefaultPredictivePop() {
        val transition = resolveBiliPaiNavDisplayPredictivePopRouteTransition(
            motionMode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
            cardTransitionEnabled = true,
            sourceMetadata = BiliPaiNavSourceMetadata(
                sourceKey = "history:BV1",
                sourceRoute = "history",
                clickedBoundsRecorded = true,
                cardFullyVisible = true
            ),
            fromKey = BiliPaiNavKey.VideoDetail("BV2", sourceRoute = "history"),
            toKey = BiliPaiNavKey.History
        )

        assertEquals(BiliPaiNavRouteTransition.NAV_DISPLAY_DEFAULT_PREDICTIVE, transition)
    }

    @Test
    fun sharedElementReady_homeVideoForward_prefersNoOpRouteLayer() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.Home,
            toKey = BiliPaiNavKey.VideoDetail("BV1", sourceRoute = "home"),
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.NONE,
            cardTransitionEnabled = true,
            sharedTransitionReady = true
        )

        assertEquals(BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun classicCardMode_interceptsSystemBackSoNavDisplayDoesNotOwnPrediction() {
        val decision = resolveBiliPaiNavMotionDecision(
            fromKey = BiliPaiNavKey.VideoDetail("BV1"),
            toKey = BiliPaiNavKey.Home,
            predictiveBackAnimationStyle = PredictiveBackAnimationStyle.NONE,
            cardTransitionEnabled = true,
            sharedTransitionReady = false
        )

        assertEquals(BiliPaiNavMotionMode.CLASSIC_CARD, decision.mode)
        assertEquals(BiliPaiNavRouteTransition.CLASSIC_CARD, decision.routeTransition)
        assertTrue(decision.interceptSystemBack)
    }

    @Test
    fun appBackActionInterception_winsEvenWhenPredictiveBackIsEnabled() {
        assertTrue(
            shouldInterceptSystemBackForNavigation3(
                mode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
                appBackActionRequiresInterception = true
            )
        )
        assertFalse(
            shouldInterceptSystemBackForNavigation3(
                mode = BiliPaiNavMotionMode.PREDICTIVE_NAV_DISPLAY,
                appBackActionRequiresInterception = false
            )
        )
    }
}
