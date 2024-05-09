package com.test.networkstat.database.entities

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
data class AppDataUsage(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "app_uid") val appUid: Int,
    @ColumnInfo(name = "app_identifier") val appIdentifier: String,
    @ColumnInfo(name = "network_type") val networkType: Int,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long,
    @ColumnInfo(name = "app_tx") val appTx: Long,
    @ColumnInfo(name = "app_rx") val appRx: Long,
    @ColumnInfo(name = "tx_packets") val txPackets: Long,
    @ColumnInfo(name = "rx_packets") val rxPackets: Long,
    @ColumnInfo(name = "r_start_time") val rStartTime: String,
    @ColumnInfo(name = "r_end_time") val rEndTime: String,
    @ColumnInfo(name = "r_app_tx") val rAppTx: String,
    @ColumnInfo(name = "r_app_rx") val rAppRx: String,
)

@Dao
interface AppDataUsageDao {
    @Query("SELECT * FROM AppDataUsage")
    fun getAll(): List<AppDataUsage>

    @Query("SELECT * FROM AppDataUsage where start_time >= :startTime and end_time <= :endTime")
    fun getByTime(startTime: Long, endTime: Long): List<AppDataUsage>

    @Query("SELECT * FROM AppDataUsage where start_time >= :startTime and end_time <= :endTime and network_type == :networkType")
    fun getByTimeAndType(networkType: Int, startTime: Long, endTime: Long): List<AppDataUsage>

    @Query("SELECT * FROM AppDataUsage limit 1")
    fun getFirstRow(): AppDataUsage

    @Insert
    fun insertAll(appUsageList: List<AppDataUsage>)

    @Delete
    fun delete(configuration: AppDataUsage)

    @Query("DELETE from AppDataUsage")
    fun deleteAll()
}