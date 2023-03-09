package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class UserRoleModel(

	@field:SerializedName("role")
	val role: String? = null,

	@field:SerializedName("user_id")
	val userId: Int? = null
)
