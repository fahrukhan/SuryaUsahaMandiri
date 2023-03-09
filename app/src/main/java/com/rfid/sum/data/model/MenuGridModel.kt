package com.rfid.sum.data.model

data class MenuGridModel(
    val title: String,
    val roleName: String,
    val icon: Int,
    var qty: Int,
    val color: Int,
    val clazz: Class<*>? = null,
    val needReader: Boolean = false,
    val reqAuthorization: Boolean = false
)
