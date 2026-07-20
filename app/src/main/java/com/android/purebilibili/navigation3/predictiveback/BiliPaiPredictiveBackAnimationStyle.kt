package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackAnimationStyle(val storageValue: String) {
    DEFAULT("default"),
    SCALE("scale"),
    AOSP("aosp"),
    CLASSIC("classic");

    companion object {
        fun fromStorageValue(value: String?): BiliPaiPredictiveBackAnimationStyle {
            return entries.find { it.storageValue == value } ?: SCALE
        }
    }
}