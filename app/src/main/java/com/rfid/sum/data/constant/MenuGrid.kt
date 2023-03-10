package com.rfid.sum.data.constant

import com.rfid.sum.R
import com.rfid.sum.activities.ui.identify.IdentifyActivity
import com.rfid.sum.activities.ui.opname.StockOpname
import com.rfid.sum.activities.ui.outbound.OutboundActivity
import com.rfid.sum.activities.ui.packing.PackingListActivity
import com.rfid.sum.activities.ui.search.SearchActivity
import com.rfid.sum.activities.ui.stock.StockInfo
import com.rfid.sum.activities.ui.tag_register.TagRegActivityV3
import com.rfid.sum.activities.ui.tag_update.TagUpdateActivity
import com.rfid.sum.activities.ui.toti.TotiActivity
import com.rfid.sum.data.model.MenuGridModel

object MenuGrid {

    val menu = listOf(
//        MenuGridModel(
//            "API TEST",
//            R.drawable.ic_api_24,
//            0,
//            R.color.orange_300,
//            ApiTestActivity::class.java,
//            false
//        ),
        MenuGridModel(
            "TAG REG",
            "rfid-registration",
            R.drawable.ic_tag_reg,
            0,
            R.color.red_300,
            TagRegActivityV3::class.java,
            false,
            reqAuthorization = true
        ),
        MenuGridModel(
            "UPDATE",
            "rfid-registration",
            R.drawable.ic_rfid_tag_update,
            0,
            R.color.red_300,
            TagUpdateActivity::class.java,
            false,
            reqAuthorization = true
        ),
        MenuGridModel(
            "TRANSFER",
            "relocation",
            R.drawable.ic_relocation,
            0,
            R.color.purple_300,
            TotiActivity::class.java,
            true,
            reqAuthorization = true
        ),
        MenuGridModel(
            "OUTBOUND",
            "outbound",
            R.drawable.ic_outbound,
            0,
            R.color.lightGreen_300,
            PackingListActivity::class.java,
            false,
            reqAuthorization = true
        ),
        MenuGridModel(
            "STOCK OPNAME",
            "stock-opname",
            R.drawable.ic_stock_opname,
            0,
            R.color.indigo_300,
            StockOpname::class.java,
            true,
            reqAuthorization = false
        ),
        MenuGridModel(
            "STOCK INFO",
            "stock-info",
            R.drawable.ic_stock_info,
            0,
            R.color.lightBlue_300,
            StockInfo::class.java,
            false,
            reqAuthorization = false
        ),
        MenuGridModel(
            "IDENTIFY",
            "identify",
            R.drawable.ic_identify,
            0,
            R.color.teal_300,
            IdentifyActivity::class.java,
            false,
            reqAuthorization = false
        ),
        MenuGridModel(
            "TAG SEARCH",
            "tag-search",
            R.drawable.ic_tag_searching,
            0,
            R.color.lightGreen_300,
            SearchActivity::class.java,
            true,
            reqAuthorization = false
        )
//    ,MenuGridModel(
//            "PACKING LIST",
//            "packing_list",
//            R.drawable.ic_tag_searching,
//            0,
//            R.color.lightGreen_300,
//            PackingListActivity::class.java,
//            true,
//            reqAuthorization = false
//        )
    )
}