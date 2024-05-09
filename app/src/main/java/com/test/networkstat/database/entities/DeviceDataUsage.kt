package com.test.networkstat.database.entities

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
data class DeviceDataUsage(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "network_type") val networkType: Int,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long,
    @ColumnInfo(name = "tx_bytes") val txBytes: Long,
    @ColumnInfo(name = "rx_bytes") val rxBytes: Long,
    @ColumnInfo(name = "tx_packets") val txPackets: Long,
    @ColumnInfo(name = "rx_packets") val rxPackets: Long,
    @ColumnInfo(name = "r_start_time") val rStartTime: String,
    @ColumnInfo(name = "r_end_time") val rEndTime: String,
    @ColumnInfo(name = "r_tx") val rTx: String,
    @ColumnInfo(name = "r_rx") val rRx: String,
)

@Dao
interface DeviceDataUsageDao {
    @Query("SELECT * FROM DeviceDataUsage")
    fun getAll(): List<DeviceDataUsage>

    @Query("SELECT * FROM DeviceDataUsage where start_time >= :startTime and end_time <= :endTime")
    fun getByTime(startTime: Long, endTime: Long): List<DeviceDataUsage>

    @Query("SELECT * FROM DeviceDataUsage where start_time >= :startTime and end_time <= :endTime and network_type == :networkType")
    fun getByTimeAndType(networkType: Int, startTime: Long, endTime: Long): List<DeviceDataUsage>

    @Insert
    fun insert(appUsageList: DeviceDataUsage)

    @Delete
    fun delete(configuration: DeviceDataUsage)

    @Query("DELETE from DeviceDataUsage")
    fun deleteAll()
}