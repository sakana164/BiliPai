package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals

class BiliPaiNavEntryContentPolicyTest {

    @Test
    fun topLevelKeysResolveToDedicatedContentRoles() {
        assertEquals(BiliPaiNavEntryContentRole.HOME, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Home))
        assertEquals(BiliPaiNavEntryContentRole.DYNAMIC, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Dynamic))
        assertEquals(BiliPaiNavEntryContentRole.SEARCH, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Search))
        assertEquals(BiliPaiNavEntryContentRole.SETTINGS, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Settings))
        assertEquals(BiliPaiNavEntryContentRole.PROFILE, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Profile))
        assertEquals(BiliPaiNavEntryContentRole.HISTORY, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.History))
        assertEquals(BiliPaiNavEntryContentRole.FAVORITE, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Favorite))
        assertEquals(BiliPaiNavEntryContentRole.WATCH_LATER, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.WatchLater))
        assertEquals(BiliPaiNavEntryContentRole.LOGIN, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Login))
        assertEquals(BiliPaiNavEntryContentRole.STORY, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Story))
        assertEquals(BiliPaiNavEntryContentRole.PARTITION, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Partition))
    }

    @Test
    fun videoDetailKeyResolvesToDedicatedContentRole() {
        assertEquals(
            BiliPaiNavEntryContentRole.VIDEO_DETAIL,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.VideoDetail("BV1"))
        )
    }

    @Test
    fun remainingDetailKeysStayDeferredUntilTheirLegacyRouteBodiesAreExtracted() {
        assertEquals(BiliPaiNavEntryContentRole.CATEGORY, resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Category(1)))
        assertEquals(
            BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE,
            resolveBiliPaiNavEntryContentRole(BiliPaiNavKey.Live(roomId = 1L))
        )
    }
}
