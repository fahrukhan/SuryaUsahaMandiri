package com.rfid.sum.data.model

import com.google.gson.annotations.SerializedName

data class BatchModel(

    @field:SerializedName("prddate")
    val prdDate: String? = null,

    @field:SerializedName("batchno")
    val batchNo: String? = null,

    @field:SerializedName("wrhsname")
    val wrhsName: String? = null,

    @field:SerializedName("batchexpdate")
    val batchExpDate: String? = null,

    @field:SerializedName("prdnmbr")
    val prdNumber: String? = null,

    @field:SerializedName("prodcode")
    val prodCode: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("prodname")
    val prodName: String? = null,

    @field:SerializedName("startdate")
    val startDate: String? = null,

    @field:SerializedName("in_stock")
    val inStock: Int? = null,

    @field:SerializedName("resultseq")
    val resultSeq: Int? = null,

    @field:SerializedName("tid")
    var tid: String? = null,

    @field:SerializedName("lot")
    val lot: String? = null,

    @field:SerializedName("uomcode")
    val uomCode: String? = null,

    @field:SerializedName("enddate")
    val endDate: String? = null,

    @field:SerializedName("cuscolor")
    val cusColor: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("transdtbatchid")
    val transDtBatchId: Int? = null,

    @field:SerializedName("batchinqty")
    val batchInQty: String? = null,

    @field:SerializedName("labeljual")
    val labeljual: String? = null,

    @field:SerializedName("so_verified_at")
    val soVerifiedAt: Any? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("wrhscode")
    var wrhsCode: String? = null,

    @field:SerializedName("qty")
    val qty: Int? = null,

    @field:SerializedName("warehouse")
    var warehouse: WarehouseModel? = null,

    @field:SerializedName("transdestnmbr")
    var transDestNumber: String? = null,

    @field:SerializedName("transdestdate")
    var transDestDate: String? = null,

    @field:SerializedName("batchinqty_registration")
    var batchInQtyReg: String? = null,

    @field:SerializedName("uomcode_registration")
    var uomCodeReg: String? = null,

    //status outbound
    @field:SerializedName("status")
    var status: Boolean? = null,

)
