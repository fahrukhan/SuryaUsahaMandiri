package com.rfid.sum.rest

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.rfid.sum.rest.model.Response
import com.rfid.sum.rest.model.ResponseModel
import com.rfid.sum.utils.Logger
import org.json.JSONObject
import java.nio.charset.Charset

class Volley(context: Context) {
    private val queue = Volley.newRequestQueue(context)

    fun getData(url: String, listener: NciApiListener, token: String? = null, key: Int? = null) {
        with(queue) {
            Logger.request(url)
            val stringRequest = object : StringRequest(Method.GET, url,
                { json ->
                    println("RESPONSE: $json")
                    val response = Gson().fromJson(json, Response::class.java)
                    listener.onResponse(response, response.message, key)
                },
                { error ->
                    val res = error.networkResponse
                    if (error is ServerError && res != null) {
                        val charset: Charset = Charsets.UTF_8
                        val resString = String(res.data, charset)
                        val response = Gson().fromJson(resString, Response::class.java)
                        listener.onResponse(response, response.message)
                    } else {
                        listener.onResponse(NCI_NULL_RESPONSE, onErrorResponse(error))
                    }

                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Mozilla/5.0"
                    )
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }

            }
            add(stringRequest)
        }
    }

    fun getData(
        url: String,
        json: JSONObject,
        listener: SumApiListener,
        token: String? = null,
        key: Int? = null
    ) {
        Logger.info("REQUEST: $json")
        with(queue) {
            val request = object : JsonObjectRequest(
                Method.GET,
                url,
                json,
                { json ->
                    println("RESPONSE: $json")
                    if (json == null || !json.has("success")) {
                        listener.onResponse(NULL_RESPONSE, CANT_CONNECT_TO_SERVER)
                    } else {
                        val response = Gson().fromJson(json.toString(), ResponseModel::class.java)
                        listener.onResponse(response, response.msgDesc!!, key)
                    }
                },
                { error ->
                    val res = error.networkResponse
                    if (error is ServerError && res != null) {
                        val charset: Charset = Charsets.UTF_8
                        val resString = String(res.data, charset)
                        val response = Gson().fromJson(resString, ResponseModel::class.java)
                        listener.onResponse(response, response.msgDesc!!)
                    } else {
                        listener.onResponse(NULL_RESPONSE, onErrorResponse(error))
                    }

                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Content-Type" to "application/json"
                    )
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            add(request)
        }
    }

    fun getData2(url: String, listener: SumApiListener, token: String? = null, key: Int? = null) {
        //Logger.infoLog("REQUEST: $json")
        with(queue) {
            val request = object : JsonObjectRequest(
                Method.GET,
                url,
                null,
                { json ->
                    println("RESPONSE: $json")
                    if (json == null || !json.has("success")) {
                        listener.onResponse(NULL_RESPONSE, CANT_CONNECT_TO_SERVER)
                    } else {
                        val response = Gson().fromJson(json.toString(), ResponseModel::class.java)
                        listener.onResponse(response, response.msgDesc!!, key)
                    }
                },
                { error ->
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                    listener.onResponse(NULL_RESPONSE, onErrorResponse(error))
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Mozilla/5.0"
                    )
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            add(request)
        }
    }

    fun getDataById(
        url: String,
        id: String,
        listener: NciApiListener,
        token: String? = null,
        key: Int? = null,
        timeOut: Int = 10000,
        tryCount: Int = 1
    ) {
        val urlId = "$url/$id"
        Logger.info("REQUEST: $urlId")
        with(queue) {
            val stringRequest = object : StringRequest(Method.GET, urlId,
                { json ->
                    // Display the first 500 characters of the response string.
                    println("RESPONSE: $json")
                    val response = Gson().fromJson(json, Response::class.java)
                    listener.onResponse(response, response.message, key)
                },
                { error ->
                    val res = error.networkResponse
                    if (error is ServerError && res != null) {
                        when (res.statusCode) {
                            500 -> {
                                listener.onResponse(Response(null, false, "Server Error"), "Server Error")
                            }
                            else -> {
                                val charset: Charset = Charsets.UTF_8
                                val resString = String(res.data, charset)
                                val response = Gson().fromJson(resString, Response::class.java)
                                listener.onResponse(response, response.message, key)
                            }
                        }
                    } else {
                        listener.onResponse(NCI_NULL_RESPONSE, onErrorResponse(error), key)
                    }
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")

                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Mozilla/5.0"
                    )
                }
            }
            stringRequest.retryPolicy = object: RetryPolicy{
                override fun getCurrentTimeout() = timeOut
                override fun getCurrentRetryCount() = tryCount
                override fun retry(error: VolleyError?) {
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                }
            }
            add(stringRequest)
        }
    }

    fun sumPostData(
        url: String,
        json: JSONObject,
        listener: SumApiListener,
        token: String? = null,
        key: Int? = null
    ) {
        Logger.request("$json")
        with(queue) {
            val request = object : JsonObjectRequest(
                Method.POST,
                url,
                json,
                { json ->
                    println("RESPONSE: $json")
                    if (json == null || !json.has("success")) {
                        listener.onResponse(NULL_RESPONSE, CANT_CONNECT_TO_SERVER)
                    } else {
                        val response = Gson().fromJson(json.toString(), ResponseModel::class.java)
                        listener.onResponse(response, response.msgDesc!!, key)
                    }
                },
                { error ->
                    val res = error.networkResponse
                    if (error is ServerError && res != null) {
                        val charset: Charset = Charsets.UTF_8
                        val resString = String(res.data, charset)
                        val response = Gson().fromJson(resString, ResponseModel::class.java)
                        listener.onResponse(response, onErrorResponse(error))
                    } else {
                        listener.onResponse(NULL_RESPONSE, onErrorResponse(error))
                    }
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                    println(error.message)
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Android",
                        "Content-Type" to "application/json"
                        //"Accept-Encoding" to "gzip, deflate, br"
                    )
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            add(request)
        }
    }

    fun nciPostData(
        url: String,
        json: JSONObject,
        listener: NciApiListener,
        token: String? = null,
        key: Int? = null
    ) {
        Logger.request("$json")
        with(queue) {
            val request = object : JsonObjectRequest(
                Method.POST,
                url,
                json,
                { json ->
                    println("RESPONSE: $json")
                    if (json == null || !json.has("success")) {
                        listener.onResponse(NCI_NULL_RESPONSE, CANT_CONNECT_TO_SERVER)
                    } else {
                        val response = Gson().fromJson(json.toString(), Response::class.java)
                        listener.onResponse(response, response.message, key)
                    }
                },
                { error ->
                    val res = error.networkResponse
                    if (error is ServerError && res != null) {
                        when (res.statusCode) {
                            500 -> {
                                listener.onResponse(Response(null, false, "Server Error"), "Server Error")
                            }
                            else -> {
                                val charset: Charset = Charsets.UTF_8
                                val resString = String(res.data, charset)
                                val response = Gson().fromJson(resString, Response::class.java)
                                listener.onResponse(response, onErrorResponse(error))
                            }
                        }

                    } else {
                        listener.onResponse(NCI_NULL_RESPONSE, onErrorResponse(error))
                    }
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                    println(error.message)
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Android",
                        "Content-Type" to "application/json"
                        //"Accept-Encoding" to "gzip, deflate, br"
                    )
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            add(request)
        }
    }



    fun putData(
        url: String,
        id: String,
        json: JSONObject,
        listener: NciApiListener,
        token: String? = null,
        key: Int? = null
    ) {
        Logger.request("REQUEST: $json")
        with(queue) {
            val request = object : JsonObjectRequest(
                Method.PUT,
                "$url/$id",
                json,
                { json ->
                    Logger.response("$json")
                    if (json == null || !json.has("success")) {
                        listener.onResponse(NCI_NULL_RESPONSE, CANT_CONNECT_TO_SERVER)
                    } else {
                        val response = Gson().fromJson(json.toString(), Response::class.java)
                        listener.onResponse(response, response.message, key)
                    }
                },
                { error ->
                    println("VOLLEY_ERROR => ${onErrorResponse(error)}")
                    listener.onResponse(NCI_NULL_RESPONSE, onErrorResponse(error))
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Mozilla/5.0"
                    )
                }

            }
            request.retryPolicy = DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            add(request)
        }
    }

    fun deleteData(url: String, id: String, listener: SumApiListener, token: String? = null) {
        with(queue) {
            val stringRequest = object : StringRequest(Method.DELETE, "$url/$id",
                { json ->
                    println("RESPONSE: $json")
                    val response = Gson().fromJson(json, ResponseModel::class.java)
                    listener.onResponse(response, response.msgDesc!!)
                },
                { error ->
                    println("VOLLEY_ERROR => $error")
                    listener.onResponse(NULL_RESPONSE, onErrorResponse(error))
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "Bearer $token",
                        "User-Agent" to "Mozilla/5.0"
                    )
                }
            }
            add(stringRequest)
        }
    }


    // Utility
    private fun onErrorResponse(it: VolleyError?): String {
        return when (it) {
            is NoConnectionError -> "No Connection"
            is TimeoutError -> "Timeout: Please check your internet connection!"
            is ServerError -> "Server Error!"
            else -> it?.message ?: CANT_CONNECT_TO_SERVER
        }
    }

    fun destroy() {
        queue.stop()
    }

    companion object {
        private const val CANT_CONNECT_TO_SERVER = "Error Connection"
        val NULL_RESPONSE = ResponseModel(
            null, 0, false,
            CANT_CONNECT_TO_SERVER
        )
        val NCI_NULL_RESPONSE = Response(null, false, CANT_CONNECT_TO_SERVER)
    }
}