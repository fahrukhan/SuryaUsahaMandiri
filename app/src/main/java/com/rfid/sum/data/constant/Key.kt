package com.rfid.sum.data.constant

import android.view.KeyEvent
import com.rfid.sum.data.model.WarehouseModel
import com.rfid.sum.rest.ApiRequest

object Key {
    const val IS_RFID_SCANNING = "isScanning"
    const val INTENT_DATA = "intentData"
    const val SESSION = "session"
    const val TOKEN = "token"
    const val TOKEN_MGT = "tokenMgt"
    const val USER_DATA = "userData"
    const val IS_LOGIN = "isLogin"

    //method code
    const val SM01 = "SM01"
    const val WH01 = "WH01"
    const val PD01 = "PD01"
    const val WH02 = "WH02"
    const val WH51 = "WH51"
    const val WH52 = "WH52"
    const val COMP_CODE = "SNB3"


    val KEY_CODE_LIST = listOf(
        KeyEvent.KEYCODE_F9, /* RFID Handle button */
        285, /* Left shortcut*/
        286, /* Right shortcut*/
    )

    fun getMethod(ar: ApiRequest): String {
        return when(ar){
            ApiRequest.Login -> SM01
            ApiRequest.GetTokenApi -> SM01
            ApiRequest.GetProductMaster -> SM01
            ApiRequest.GetWarehouseMaster -> WH01
            ApiRequest.GetCPBatchTrans -> PD01
            ApiRequest.GetPackingList -> WH02
            ApiRequest.SetBatchRFID -> WH51
            ApiRequest.SetTOTI -> WH52
        }
    }

    /**
     * @warehouseList Dummy data please delete on production
     */
    val warehouseList: List<WarehouseModel> = listOf<WarehouseModel>(
        WarehouseModel(wrhsId = 1, wrhsCode = "103", wrhsName = "GUDANG KAIN JADI"),
        WarehouseModel(wrhsId = 2, wrhsCode = "103B", wrhsName = "GUDANG JADI B GRADE"),
        WarehouseModel(wrhsId = 3, wrhsCode = "190", wrhsName = "GUDANG SAMPLE"),
        WarehouseModel(wrhsId = 4, wrhsCode = "111", wrhsName = "GUDANG EX-WARNA"),
        WarehouseModel(wrhsId = 5, wrhsCode = "103A", wrhsName = "GUDANG JADI KECIL"),
        WarehouseModel(wrhsId = 6, wrhsCode = "172", wrhsName = "GUDANG SBY"),
        WarehouseModel(wrhsId = 7, wrhsCode = "173", wrhsName = "GUDANG SBY BUNDLING"),
        WarehouseModel(wrhsId = 8, wrhsCode = "193", wrhsName = "GUDANG SAMPLE SUM"),
        WarehouseModel(wrhsId = 9, wrhsCode = "112", wrhsName = "GUDANG TRANSIT VERPACKING")
    )
    /*
    * End Of Dummy Data
    */

}