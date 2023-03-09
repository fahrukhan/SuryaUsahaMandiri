package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class WarehouseModel(

	@field:SerializedName("wrhsname")
	val wrhsName: String? = null,

	@field:SerializedName("wrhsid")
	val wrhsId: Int? = null,

	@field:SerializedName("wrhscode")
	val wrhsCode: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,
)
