package com.rfid.sum.data.support

import com.rfid.sum.rest.model.TokenModel

interface TokenInitialize {
    fun activeToken(mToken: TokenModel?)
    fun freshToken(mToken: TokenModel?)
    fun refreshingToken()
    fun failedRefresh(msg: String)
}