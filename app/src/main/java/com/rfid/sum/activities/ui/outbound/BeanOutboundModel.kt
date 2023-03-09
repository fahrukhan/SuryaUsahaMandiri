package com.rfid.sum.activities.ui.outbound

import com.google.gson.annotations.SerializedName
import com.rfid.sum.data.model.CustomerModel
import com.rfid.sum.data.model.UserModel

data class BeanOutboundModel(

	@field:SerializedName("at")
	val at: String? = null,

	@field:SerializedName("receipt_number")
	val receiptNumber: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("from_data")
	val fromData: UserModel? = null,

	@field:SerializedName("to_data")
	val toData: CustomerModel? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("details")
	val details: List<DetailsItem?>? = null,

	@field:SerializedName("id")
	val id: Int? = null
)

data class DetailsItem(
	@field:SerializedName("prdnmbr")
	val prdNumb: String? = null,

	@field:SerializedName("qty")
	val qty: Int? = null,

	@field:SerializedName("prodname")
	val prodName: String? = null,

	@field:SerializedName("outbound_id")
	val outboundId: Int? = null
)
