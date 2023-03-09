package com.rfid.sum.rest.model

import com.google.gson.annotations.SerializedName

data class TokenResponseModel(

	@field:SerializedName("msgcode")
	val msgcode: Int? = null,

	@field:SerializedName("requestid")
	val requestid: Int? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("msgdesc")
	val msgdesc: String? = null,

	@field:SerializedName("token")
	val token: String? = null
)
