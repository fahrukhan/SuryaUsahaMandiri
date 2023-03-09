package com.rfid.sum.data.constant

object Url {
    //Please change on production
    const val DEVELOPMENT_MODE = true

    //development
    private const val BASE_SUM_API  = "http://202.150.136.54/erp/api/"
    private const val BASE_NCI_API_V1 = "http://149.129.232.88/surya_usaha_mandiri_api/public/api/v1"

    //NCI
    const val LOGIN = "${BASE_NCI_API_V1}/login"
    const val USER = "${BASE_NCI_API_V1}/user"
    const val ITEMS = "${BASE_NCI_API_V1}/items"
    const val RELOCATION = "${BASE_NCI_API_V1}/relocations"
    const val RFID_LOGS = "${BASE_NCI_API_V1}/rfid-logs/tid"
    const val CUSTOMER ="${BASE_NCI_API_V1}/customers"
    const val OUTBOUNDS = "${BASE_NCI_API_V1}/outbounds"
    const val STOCK_OPNAME = "${BASE_NCI_API_V1}/stockopnames"
    const val BATCH_SYNC = "${BASE_NCI_API_V1}/cpbatchs/sync"
    const val PRODUCT_SYNCED = "${BASE_NCI_API_V1}/cpbatchs/prdnmbr"
    const val PACKING_LIST ="$BASE_NCI_API_V1/packinglists"
    //id DOL/2302/0866 -> replace '/' to '-' => DOL-2302-0866
    const val PACKING_LIST_BY_DOL ="$BASE_NCI_API_V1/packinglists/transdestnmbr"

    const val ITEMS_BY_RFID = "$ITEMS/tid"
    const val ITEMS_REGISTERED = "$ITEMS/prdnmbr"
    const val ITEMS_BY_WH = "${ITEMS}/wrhscode"
    const val PUT_BATCH = "${ITEMS}/batchno"

    //SUM
    const val GET_TOKEN = "${BASE_SUM_API}GetTokenApi"
    const val GET_API = "${BASE_SUM_API}GetApi"

}