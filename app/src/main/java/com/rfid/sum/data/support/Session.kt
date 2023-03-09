package com.rfid.sum.data.support

import android.content.Context
import com.google.gson.Gson
import com.rfid.sum.data.constant.Key.IS_LOGIN
import com.rfid.sum.data.constant.Key.SESSION
import com.rfid.sum.data.constant.Key.USER_DATA
import com.rfid.sum.data.model.UserModel

class Session(context: Context) {

    private val pref = Preference(context, SESSION)

    fun saveUser(user: UserModel) {
        val userToPref = Gson().toJson(user)

        with(pref) {
            writeString(USER_DATA, userToPref)
            writeBool(IS_LOGIN, true)
        }
    }

    fun getUser(): UserModel? {
        val u = pref.readString(USER_DATA) ?: ""
        return if (u.isEmpty()) null
        else Gson().fromJson(u, UserModel::class.java)
    }

    fun isLogin(): Boolean {
        return pref.readBool(IS_LOGIN, false)
    }

    fun clear() {
        with(pref) {
            remove(USER_DATA)
            writeBool(IS_LOGIN, false)
        }
    }


}

