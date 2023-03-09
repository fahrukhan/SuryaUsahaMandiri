package com.rfid.sum.data.support

import android.content.Context

class Preference(context: Context, name: String) {
    private val pref = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun writeString(key: String, value: String?){
        with(pref.edit()){
            putString(key, value)
            apply()
        }
    }

    fun readString(key: String): String? {
        return pref.getString(key, "")
    }

    fun writeInt(key: String, value: Int){
        with(pref.edit()){
            putInt(key, value)
            commit()
        }
    }

    fun readInt(key: String, defValue: Int): Int {
        return pref.getInt(key, defValue)
    }

    fun writeBool(key: String, value: Boolean){
        with(pref.edit()){
            putBoolean(key, value)
            commit()
        }
    }

    fun readBool(key: String, defValue: Boolean): Boolean{
        return pref.getBoolean(key, defValue)
    }

    fun writeLong(key: String, value: Long){
        with(pref.edit()){
            putLong(key, value)
            commit()
        }
    }

    fun readLong(key: String, defValue: Long): Long{
        return pref.getLong(key, defValue)
    }

    fun remove(key: String){
        with(pref.edit()){
            remove(key).apply()
        }
    }
}