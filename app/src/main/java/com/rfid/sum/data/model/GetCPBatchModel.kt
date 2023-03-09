package com.rfid.sum.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GetCPBatchModel(

	@field:SerializedName("prddate")
	val prdDate: String? = null,

	@field:SerializedName("batchno")
	val batchNo: String? = null,

	@field:SerializedName("wrhsname")
	val wrhsName: String? = null,

	@field:SerializedName("batchexpdate")
	val batchExpDate: Any? = null,

	@field:SerializedName("prdnmbr")
	val prdNmbr: String? = null,

	@field:SerializedName("prodcode")
	val prodCode: String? = null,

	@field:SerializedName("prodname")
	val prodName: String? = null,

	@field:SerializedName("startdate")
	val startDate: String? = null,

	@field:SerializedName("resultseq")
	val resultSeq: Int? = null,

	@field:SerializedName("lot")
	val lot: String? = null,

	@field:SerializedName("uomcode")
	val uomCode: String? = null,

	@field:SerializedName("enddate")
	val endDate: String? = null,

	@field:SerializedName("transdtbatchid")
	val transDtBatchId: Int? = null,

	@field:SerializedName("batchinqty")
	val batchInQty: Double? = null,

	@field:SerializedName("wrhscode")
	val wrhsCode: String? = null,

	@field:SerializedName("labeljual")
	val labelJual: String? = null,

	@field:SerializedName("cuscolor")
	val cusColor: String? = null,

	var tid: String? = null,

	@Transient
	var totalBatch: Int = 0,

)
