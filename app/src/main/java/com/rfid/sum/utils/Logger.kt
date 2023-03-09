package com.rfid.sum.utils

import android.content.ContentValues
import android.util.Log
import com.rfid.sum.data.constant.Url

object Logger {
    fun info(msg: Any){
        if (Url.DEVELOPMENT_MODE)  Log.i(ContentValues.TAG,"INFO ==> $msg")
    }

    fun warning(msg: Any){
        if (Url.DEVELOPMENT_MODE) Log.w(ContentValues.TAG,"WARNING ==> $msg")
    }

    fun error(msg: Any, ignoreDev: Boolean = Url.DEVELOPMENT_MODE){
        if (ignoreDev) Log.e(ContentValues.TAG,"ERROR ==> $msg")
    }

    fun request(msg: String){
        if (Url.DEVELOPMENT_MODE) Log.i(ContentValues.TAG,"REQUEST ==> $msg")
    }

    fun response(msg: String){
        if (Url.DEVELOPMENT_MODE) Log.i(ContentValues.TAG,"RESPONSE ==> $msg")
    }
}