package com.test.networkstat.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.test.networkstat.App
import com.test.networkstat.BuildConfig
import com.test.networkstat.database.entities.AppDataUsage
import com.test.networkstat.database.entities.AppDataUsageDao

@Database(entities = [AppDataUsage::class], version = 1)
abstract class RoomDB : RoomDatabase() {
    abstract fun appDataUsageDao(): AppDataUsageDao

    companion object {
        private lateinit var db: RoomDB

        fun getDatabase(): RoomDB {
            if (!this::db.isInitialized) {
                synchronized(RoomDB::class) {
                    db = Room.databaseBuilder(
                        App.getInstance().applicationContext,
                        RoomDB::class.java, BuildConfig.APPLICATION_ID
                    ).build()
                }
            }
            return db
        }
    }
}