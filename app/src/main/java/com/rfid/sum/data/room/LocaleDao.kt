package com.rfid.sum.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rfid.sum.data.room.model.SumApiHistoryModel
import kotlinx.coroutines.flow.Flow

@Dao
interface LocaleDao {
    @Query("SELECT * FROM sum_api_history")
    fun allSumApiHistory(): Flow<List<SumApiHistoryModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveApiHistory(history: SumApiHistoryModel)

    suspend fun saveApiHistoryWithTimestamp(data: SumApiHistoryModel) {
        saveApiHistory(data.apply{
            createdAt = System.currentTimeMillis()
            updatedAt = System.currentTimeMillis()
        })
    }


}