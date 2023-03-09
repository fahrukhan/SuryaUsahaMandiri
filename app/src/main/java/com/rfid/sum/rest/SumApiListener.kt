package com.rfid.sum.rest

import com.rfid.sum.rest.model.ResponseModel

interface SumApiListener {
    fun onResponse(response: ResponseModel, errorMsg: String, key: Int? = null)
}