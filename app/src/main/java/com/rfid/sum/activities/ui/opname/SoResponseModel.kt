package com.rfid.sum.activities.ui.opname

import com.google.gson.annotations.SerializedName

data class SoResponseModel(

	@field:SerializedName("at")
	val at: String? = null,

	@field:SerializedName("receipt_number")
	val receiptNumber: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("user_id")
	val userId: Int? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("wrhscode")
	val wrhscode: String? = null
)
