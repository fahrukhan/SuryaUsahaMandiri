package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class GetProductMasterModel(

	@field:SerializedName("hfeelname")
	val hFeelName: Any? = null,

	@field:SerializedName("brandtype")
	val brandType: Any? = null,

	@field:SerializedName("gradedesc")
	val gradeDesc: Any? = null,

	@field:SerializedName("prodcode")
	val prodCode: String? = null,

	@field:SerializedName("colorcode")
	val colorCode: Any? = null,

	@field:SerializedName("gradecode")
	val gradeCode: Any? = null,

	@field:SerializedName("prodname")
	val prodName: String? = null,

	@field:SerializedName("prodid")
	val prodId: Int? = null,

	@field:SerializedName("brandname")
	val brandName: Any? = null,

	@field:SerializedName("brandcode")
	val brandCode: Any? = null,

	@field:SerializedName("hfeelcode")
	val hFeelCode: Any? = null,

	@field:SerializedName("colorname")
	val colorName: Any? = null,

	@field:SerializedName("uomcode")
	val uomCode: String? = null,

	@field:SerializedName("cuscolor")
	val cusColor: Any? = null,

	@field:SerializedName("labeljual")
	val labelJual: Any? = null
)