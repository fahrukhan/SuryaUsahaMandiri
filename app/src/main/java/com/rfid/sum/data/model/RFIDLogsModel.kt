package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class RFIDLogsModel(

	@field:SerializedName("note")
	val note: String? = null,

	@field:SerializedName("batchno")
	val batchno: String? = null,

	@field:SerializedName("at")
	val at: String? = null,

	@field:SerializedName("activity")
	val activity: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("user_id")
	val userId: Int? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("user")
	val user: NciUserModel? = null,

	@field:SerializedName("tid")
	val tid: String? = null
)
