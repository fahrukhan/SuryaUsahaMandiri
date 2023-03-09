package com.rfid.sum.rest

import android.content.Context
import com.google.gson.Gson
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.support.Session
import com.rfid.sum.data.constant.Key.COMP_CODE

class ParamBuilder(val context: Context) {
    private val session = Session(context)

    /**
     * @Param [parameter] => check [ApiRequest] expression
     * @param [parameterList] => check [ApiRequest] expression
     * @param [fromWarehouse] => "000"
     * @param [toWarehouse] => "001"
     * @param [trfDate] => "20220831"
     *
     * date format for all param 'yyyyMMdd'
     */
     fun getParam(
        ar: ApiRequest,
        token: String? = null,
        parameter: Map<String, String> = mapOf(),
        parameterList: List<Any> = listOf(),
        fromWarehouse: String? = null,
        toWarehouse: String? = null,
        trfDate: String? = null
    ): Map<String, Any> {

        val method = Key.getMethod(ar)

        val params = mutableMapOf<String, Any>(
            "CompCode" to COMP_CODE,
            "Method" to method
        )

        if (ar == ApiRequest.Login){
            return params
        }
        val user = session.getUser()!!

        params["UserId"] = "Proint"
        params["Password"] = "123456"
        params["AccessToken"] = token!!

//        if (token == null){
//            tos.error("Access Denied!")
//            return null
//        }

        when(ar){
            ApiRequest.GetCPBatchTrans, ApiRequest.GetPackingList -> {
                // sParameter => {"StartDate":"20211101","EndDate":"20211231"}

                //val sParameter = Gson().toJson(parameter)
                params["Parameter"] = parameter
            }
            ApiRequest.SetBatchRFID -> {
                // sParamList => [{"BatchNo":"BATCH001","RFID":"RFID001"}]

                //val sParamList = Gson().toJson(parameterList)
                params["ParameterList"] = parameterList
            }
            ApiRequest.SetTOTI -> {
                // sParamList => :[{"prodcode":"FIG00004.01","batchno":"12345","qty":"2","uom":"M"}]

                val sParamList = Gson().toJson(parameterList)
                params["ParameterList"] = sParamList
            }
            else -> { /* no-op */ }
        }

        return params
    }

    fun removedTokenParams(params: Map<String, Any>): String? {
        val nowParams: MutableMap<String, Any> = mutableMapOf()
        nowParams.putAll(params)
        nowParams.remove("AccessToken")
        return Gson().toJson(nowParams)
    }

}