package com.android.purebilibili.navigation3

internal enum class BiliPaiNavEntryContentRole {
    HOME,
    DYNAMIC,
    SEARCH,
    SETTINGS,
    PROFILE,
    VIDEO_DETAIL,
    HISTORY,
    FAVORITE,
    WATCH_LATER,
    LOGIN,
    STORY,
    PARTITION,
    CATEGORY,
    DEFERRED_LEGACY_ROUTE
}

internal fun resolveBiliPaiNavEntryContentRole(key: BiliPaiNavKey): BiliPaiNavEntryContentRole {
    return when (key) {
        BiliPaiNavKey.Home -> BiliPaiNavEntryContentRole.HOME
        BiliPaiNavKey.Dynamic -> BiliPaiNavEntryContentRole.DYNAMIC
        BiliPaiNavKey.Search -> BiliPaiNavEntryContentRole.SEARCH
        BiliPaiNavKey.Settings -> BiliPaiNavEntryContentRole.SETTINGS
        BiliPaiNavKey.Profile -> BiliPaiNavEntryContentRole.PROFILE
        is BiliPaiNavKey.VideoDetail -> BiliPaiNavEntryContentRole.VIDEO_DETAIL
        BiliPaiNavKey.History -> BiliPaiNavEntryContentRole.HISTORY
        BiliPaiNavKey.Favorite -> BiliPaiNavEntryContentRole.FAVORITE
        BiliPaiNavKey.WatchLater -> BiliPaiNavEntryContentRole.WATCH_LATER
        BiliPaiNavKey.Login -> BiliPaiNavEntryContentRole.LOGIN
        BiliPaiNavKey.Story -> BiliPaiNavEntryContentRole.STORY
        BiliPaiNavKey.Partition -> BiliPaiNavEntryContentRole.PARTITION
        is BiliPaiNavKey.Category -> BiliPaiNavEntryContentRole.CATEGORY
        else -> BiliPaiNavEntryContentRole.DEFERRED_LEGACY_ROUTE
    }
}
