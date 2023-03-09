package com.rfid.sum.rest.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ResponseModel(

	@field:SerializedName("msgcode")
	val msgCode: Int? = null,

	@field:SerializedName("requestid")
	val requestId: Int? = null,

	@field:SerializedName("success")
	val success: Boolean = false,

	@field:SerializedName("msgdesc")
	val msgDesc: String? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("token")
	val token: String? = null,

	@field:SerializedName("data")
	val data: Any? = null
){
	inline fun <reified T> castListModel(): List<T> {
		if (data is List<*>) {
			if (data.isNotEmpty()) {
				val modelList = mutableListOf<T>()
				for (bean in data) {
					val json = Gson().toJson(bean)
					val obj = Gson().fromJson(json, T::class.java)
					modelList.add(obj)
				}
				return modelList
			}
			return listOf()
		} else {
			return listOf()
		}
	}

	fun responseToHistory(): String? {
		val response = ResponseModel(
			msgCode, requestId, success, msgDesc
		)
		return Gson().toJson(response)
	}
}
