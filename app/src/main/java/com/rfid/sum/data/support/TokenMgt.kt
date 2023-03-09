package com.rfid.sum.data.support

import android.content.Context
import com.google.gson.Gson
import com.rfid.sum.data.constant.Key
import com.rfid.sum.data.constant.Key.TOKEN_MGT
import com.rfid.sum.data.constant.Key.SM01
import com.rfid.sum.data.constant.Key.WH01
import com.rfid.sum.data.constant.Key.PD01
import com.rfid.sum.data.constant.Key.WH02
import com.rfid.sum.data.constant.Key.WH51
import com.rfid.sum.data.constant.Key.WH52
import com.rfid.sum.data.constant.Key.COMP_CODE
import com.rfid.sum.data.constant.Url
import com.rfid.sum.rest.ApiRequest
import com.rfid.sum.rest.Volley
import com.rfid.sum.rest.SumApiListener
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.rest.model.TokenModel
import org.json.JSONObject
import java.util.*

class TokenMgt(context: Context) {
    private val pref = Preference(context, TOKEN_MGT)
    private val session = Session(context)
    //private val user = session.getUser()
    private val volley = Volley(context)

    fun saveToken(tokenCode: String, token: TokenModel): Boolean{
        if (token.token == null) return false

        val gson = Gson().toJson(token)
        pref.writeString(tokenCode, gson)
        return true
    }

    fun removeToken(tokenCode: String){
        pref.remove(tokenCode)
    }

    private fun getToken(tokenCode: String): TokenModel? {
        with(pref) {
            val sGson = when (tokenCode) {
                SM01 -> { readString(SM01) ?: "" }
                WH01 -> { readString(WH01) ?: "" }
                PD01 -> { readString(PD01) ?: "" }
                WH02 -> { readString(WH02) ?: "" }
                WH51 -> { readString(WH51) ?: "" }
                WH52 -> { readString(WH52) ?: "" }
                else -> { "" }
            }

            if (sGson.isEmpty()) return null
            return Gson().fromJson(sGson, TokenModel::class.java)
        }
    }

    fun getToken(ar: ApiRequest, listener: TokenInitialize) {
        val tokenCode = Key.getMethod(ar)

        println("GET TOKEN METHOD: $tokenCode")

        if (isExpired(tokenCode)){
            listener.refreshingToken()
            refreshToken(tokenCode, object : SumApiListener{
                override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
                    with(response){
                        if (success){
                            listener.freshToken(getToken(tokenCode))
                        }else{
                            listener.failedRefresh(errorMsg)
                        }
                    }
                }
            })
        }else{
            listener.activeToken(getToken(tokenCode))
        }
    }

    private fun isExpired(tokenCode: String): Boolean {
        val  mToken = getToken(tokenCode)
        if (mToken != null) {
            val now = Calendar.getInstance()
            val sec = now.timeInMillis / 1000
            val diff = mToken.expiredAt - sec
            return diff <= 300
        }
        return true
    }

    private fun refreshToken(tokenCode: String, listener: SumApiListener) {
//        if (user == null) {
//            listener.onResponse(ResponseModel(success = false), "Please login!")
//            return
//        }

        println("REFRESH TOKEN METHOD: $tokenCode")
        val gson = Gson().toJson(
            mapOf(
                "CompCode" to COMP_CODE,
                "UserId" to "Proint",
                "Password" to "123456",
                "Method" to tokenCode
            )
        )

        volley.sumPostData(Url.GET_TOKEN, JSONObject(gson), object : SumApiListener {
            override fun onResponse(response: ResponseModel, errorMsg: String, key: Int?) {
                with(response) {
                    if (success) {
                        val cal = Calendar.getInstance()
                        val created = cal.timeInMillis / 1000
                        val expired = created + 3600;

                        val mToken = TokenModel(requestId, token, created, expired)
                        val refresh = saveToken(tokenCode, mToken)
                        if (refresh){
                            listener.onResponse(ResponseModel(success = true), "Success")
                        }else{
                            listener.onResponse(ResponseModel(success = false), "Error refreshing token!")
                        }
                    } else {
                        listener.onResponse(ResponseModel(success = false), errorMsg)
                    }
                }
            }
        })
    }
}