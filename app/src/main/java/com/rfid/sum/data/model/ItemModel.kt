package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class ItemModel(
	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("tid")
	val tid: String? = null,

	@field:SerializedName("in_stock")
	val inStock: Int? = null,

	@field:SerializedName("batchno")
	var batchNo: String? = null,

	@field:SerializedName("cp_batch_data")
	val cpBatchData: GetCPBatchModel? = null,

	@field:SerializedName("prdnmbr")
	var prodNumber: String? = null,

	@field:SerializedName("wrhscode")
	var wrhsCode: String? = null,

	@field:SerializedName("warehouse")
	var warehouse: WarehouseModel? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	//outbound details
	@field:SerializedName("prodcode")
	val prodCode: String? = null,

	@field:SerializedName("prodname")
	val prodName: String? = null,

	@field:SerializedName("outbound_id")
	val outboundId: Int? = null,
)