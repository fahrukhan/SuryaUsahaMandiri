package com.rfid.sum.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rfid.sum.data.room.model.SumApiHistoryModel
import com.rfid.sum.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SumApiHistoryModel::class], version = 1, exportSchema = true)
abstract class RoomDb: RoomDatabase() {
    abstract fun localeDao(): LocaleDao

    companion object {
        private const val DB_NAME = "sum_db"
        private var INSTANCE: RoomDb? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RoomDb {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomDb::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(RfidDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class RfidDatabaseCallback(
            private val scope: CoroutineScope
        ): RoomDatabase.Callback(){
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                INSTANCE?.let { dbs ->
                    scope.launch(Dispatchers.IO){
                        populateDatabase(dbs.localeDao())

                    }
                }
            }

        }

        suspend fun populateDatabase(rfidDao: LocaleDao){
//            val rf = DaoModel(
//                ApiRequest.SetBatchRFID.toString(),
//                "123456",
//            )
//            rfidDao.insert(rf)
            Logger.info("DB Initialize")
        }
    }
}