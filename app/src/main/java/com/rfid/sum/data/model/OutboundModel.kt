package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class OutboundModel(

	@field:SerializedName("at")
	val at: String? = null,

	@field:SerializedName("receipt_number")
	val receiptNumber: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("from_data")
	val fromData: CustomerModel? = null,

	@field:SerializedName("to_data")
	val toData: CustomerModel? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("outbound_details")
	val outboundDetails: List<ItemModel>? = null
)

