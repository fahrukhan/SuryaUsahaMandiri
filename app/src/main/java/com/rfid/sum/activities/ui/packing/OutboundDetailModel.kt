package com.rfid.sum.activities.ui.packing

import com.google.gson.annotations.SerializedName
import com.rfid.sum.data.model.BatchModel

data class OutboundDetailModel(

	@field:SerializedName("data")
	val data: List<BatchModel>? = null,

	@field:SerializedName("total_data")
	val totalData: Int? = null,

	@field:SerializedName("total_true")
	val totalTrue: Int? = null,

	@field:SerializedName("total_false")
	val totalFalse: Int? = null
)
