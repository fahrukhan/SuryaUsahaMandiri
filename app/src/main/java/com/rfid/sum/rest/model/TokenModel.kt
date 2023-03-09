package com.rfid.sum.rest.model

import com.google.gson.annotations.SerializedName

data class TokenModel(
    val requestId: Int? = null,
    var token: String? = null,
    val createdAt: Long,
    var expiredAt: Long
)
