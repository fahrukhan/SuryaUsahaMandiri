package com.rfid.sum

import android.app.Application
import com.rfid.sum.data.room.RoomDb
import com.rfid.sum.data.room.repository.DaoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class SumApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    private val database by lazy {  RoomDb.getDatabase(this, applicationScope) }
    val repository by lazy { DaoRepository(database.localeDao()) }
}