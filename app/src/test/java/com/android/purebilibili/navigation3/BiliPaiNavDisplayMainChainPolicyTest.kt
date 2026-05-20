package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertFalse

class BiliPaiNavDisplayMainChainPolicyTest {

    @Test
    fun navDisplayMainChainStartsDisabledUntilLegacyRoutesAreMapped() {
        assertFalse(shouldUseBiliPaiNavDisplayMainChain())
    }
}
