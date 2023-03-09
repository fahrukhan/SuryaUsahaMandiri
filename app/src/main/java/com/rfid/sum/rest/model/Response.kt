package com.rfid.sum.rest.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Response(
    @field:SerializedName("data")
    val data: Any?,

    @field:SerializedName("success")
    val success: Boolean,

    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("token")
    val token: String? = null,
) {

    inline fun <reified T> castModel(): T? {
        val json = Gson().toJson(data)
        val model = Gson().fromJson(json, T::class.java)
        return model as T
    }

    inline fun <reified T> castArrayListModel(): ArrayList<T>? {
        if (data is List<*>) {
            if (data.isNotEmpty()) {
                val modelList = arrayListOf<T>()
                for (bean in data) {
                    val json = Gson().toJson(bean)
                    val obj = Gson().fromJson(json, T::class.java)
                    modelList.add(obj)
                }
                return modelList
            }
            return null
        } else {
            return null
        }

    }

    inline fun <reified T> castListModel(): List<T> {
        if (data is List<*>) {
            if (data.isNotEmpty()) {
                val modelList = mutableListOf<T>()
                for (bean in data) {
                    val json = Gson().toJson(bean)
                    val obj = Gson().fromJson(json, T::class.java)
                    modelList.add(obj)
                }
                return modelList
            }
            return listOf()
        } else {
            return listOf()
        }
    }

    fun castListString(): List<String>? {
        if (data is List<*>) {
            if (data.isNotEmpty()) {
                val modelList = mutableListOf<String>()
                for (bean in data) {
                    val obj = bean as String
                    modelList.add(obj)
                }
                return modelList
            }
            return null
        } else {
            return null
        }
    }

    fun castListDouble(): List<Double>? {
        if (data is List<*>) {
            if (data.isNotEmpty()) {
                val modelList = mutableListOf<Double>()
                for (bean in data) {
                    val obj = bean as Double
                    modelList.add(obj)
                }
                return modelList
            }
            return null
        } else {
            return null
        }
    }
}


