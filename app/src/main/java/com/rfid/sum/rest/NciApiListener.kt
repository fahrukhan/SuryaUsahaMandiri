package com.rfid.sum.rest

import com.rfid.sum.rest.model.Response
import com.rfid.sum.rest.model.ResponseModel

interface NciApiListener {
    fun onResponse(response: Response, errorMsg: String, key: Int? = null)
}