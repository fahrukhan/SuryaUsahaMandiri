package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class PackingListModel(

	@field:SerializedName("batchno")
	val batchno: String? = null,

	@field:SerializedName("wrhsname")
	val wrhsname: String? = null,

	@field:SerializedName("batchexpdate")
	val batchexpdate: Any? = null,

	@field:SerializedName("prdnmbr")
	val prdnmbr: Any? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("prodname")
	val prodname: String? = null,

	@field:SerializedName("startdate")
	val startdate: Any? = null,

	@field:SerializedName("in_stock")
	val inStock: Int? = null,

	@field:SerializedName("tid")
	val tid: Any? = null,

	@field:SerializedName("lot")
	val lot: Any? = null,

	@field:SerializedName("uomcode")
	val uomcode: String? = null,

	@field:SerializedName("transdestnmbr")
	val transdestnmbr: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("so_verified_at")
	val soVerifiedAt: Any? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("prddate")
	val prddate: Any? = null,

	@field:SerializedName("prodcode")
	val prodcode: String? = null,

	@field:SerializedName("resultseq")
	val resultseq: Any? = null,

	@field:SerializedName("transseq")
	val transseq: Int? = null,

	@field:SerializedName("transdestdate")
	val transdestdate: String? = null,

	@field:SerializedName("batchoutqty")
	val batchoutqty: Double? = null,

	@field:SerializedName("enddate")
	val enddate: Any? = null,

	@field:SerializedName("cuscolor")
	val cuscolor: Any? = null,

	@field:SerializedName("transdtbatchid")
	val transdtbatchid: Int? = null,

	@field:SerializedName("batchinqty")
	val batchinqty: Any? = null,

	@field:SerializedName("labeljual")
	val labeljual: Any? = null,

	@field:SerializedName("wrhscode")
	val wrhscode: String? = null,

	// for packing list
	@field:SerializedName("qty")
	val qty: Int? = null
)
