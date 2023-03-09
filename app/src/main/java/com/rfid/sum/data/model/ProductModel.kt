package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class ProductModel(

	@field:SerializedName("hfeelname")
	val hfeelname: String? = null,

	@field:SerializedName("brandtype")
	val brandtype: Any? = null,

	@field:SerializedName("gradedesc")
	val gradedesc: String? = null,

	@field:SerializedName("prodcode")
	val prodcode: String? = null,

	@field:SerializedName("colorcode")
	val colorcode: String? = null,

	@field:SerializedName("gradecode")
	val gradecode: String? = null,

	@field:SerializedName("prodname")
	val prodname: String? = null,

	@field:SerializedName("prodid")
	val prodid: Int? = null,

	@field:SerializedName("brandname")
	val brandname: Any? = null,

	@field:SerializedName("brandcode")
	val brandcode: Any? = null,

	@field:SerializedName("hfeelcode")
	val hfeelcode: String? = null,

	@field:SerializedName("colorname")
	val colorname: String? = null,

	@field:SerializedName("uomcode")
	val uomcode: String? = null,

	@field:SerializedName("cuscolor")
	val cuscolor: String? = null,

	@field:SerializedName("labeljual")
	val labeljual: String? = null,

	@field:SerializedName("prdnmbr")
	val prodNumber: String? = null,

	@field:SerializedName("qty")
	val qty: Int? = null,
)
