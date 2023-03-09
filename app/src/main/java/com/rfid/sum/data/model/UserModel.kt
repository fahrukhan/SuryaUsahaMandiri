package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class UserModel(
//	val userId: String,
//	val pass: String,
//	val token: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("email")
	val email: String? = null,

	@field:SerializedName("user_roles")
	val user_roles: List<UserRoleModel>? = null,

	@field:SerializedName("token")
	var token: String? = null
)
