package com.rfid.sum.data.room.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sum_api_history")
data class SumApiHistoryModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,

    @ColumnInfo(name = "method")
    val method: String,

    @ColumnInfo(name = "api")
    val api: String,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "update_at", defaultValue = "CURRENT_TIMESTAMP")
    var updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "request")
    val request: String,

    @ColumnInfo(name = "response", defaultValue = "NULL")
    val response: String? = null
)
