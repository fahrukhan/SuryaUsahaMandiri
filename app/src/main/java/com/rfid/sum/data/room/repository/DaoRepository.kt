package com.rfid.sum.data.room.repository

import com.rfid.sum.data.room.LocaleDao
import com.rfid.sum.data.room.model.SumApiHistoryModel
import kotlinx.coroutines.flow.Flow

class DaoRepository(private val dao: LocaleDao) {
    val allSumApiHistory: Flow<List<SumApiHistoryModel>> = dao.allSumApiHistory()

//    suspend fun insert(model: DaoModel){
//        dao.insert(model)
//    }

    suspend fun saveApiHistory(history: SumApiHistoryModel){
        dao.saveApiHistory(history)
    }
}