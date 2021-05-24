package com.test.networkstat.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import com.test.networkstat.database.RoomDB
import com.test.networkstat.database.entities.AppDataUsage
import com.test.networkstat.database.entities.DeviceDataUsage
import com.test.networkstat.database.models.DataUsage
import com.test.networkstat.database.models.QueryConfig
import com.test.networkstat.database.models.TimePeriod
import com.test.networkstat.utils.DataUsageUtil
import com.test.networkstat.utils.Util
import java.util.concurrent.TimeUnit

class AppService : Service() {
    private val TAG = "akshay"
    private var t: Thread? = null
    private var running = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        running = true
        startThread()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        showNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun showNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notification: Notification = Notification.Builder(this, "xyz_service")
                .setContentTitle("xyz")
                .setContentText("Data collection notification")
                .build()
            // Notification ID cannot be 0.
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        running = false
        t?.interrupt()
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    private fun startThread() {
        t = Thread {
            do {
                Log.e(TAG, "startThread: ")
                val queryConfig = QueryConfig(NetworkCapabilities.TRANSPORT_CELLULAR)
                queryConfig.prevCollectionEndTime = Util.getLastCollectionEndTime(this)
                queryConfig.timePeriod = getTimePeriod(queryConfig)

                updateAppUsageDb(queryConfig, getAppUsage(queryConfig))
                updateDeviceUsageDb(queryConfig, getDeviceUsage(queryConfig))

                Util.updateLastCollectionTime(this, queryConfig.timePeriod)
                try {
                    Thread.sleep(getCollectionInterval(queryConfig))
                } catch (e: InterruptedException) {
                    Log.d(TAG, "startThread: ", e)
                }
            } while (running)
            stopSelf()
        }
        t!!.start()
    }

    private fun getTimePeriod(queryConfig: QueryConfig): TimePeriod {
        return DataUsageUtil.getStartEndTime(queryConfig.prevCollectionEndTime)
    }

    private fun getAppUsage(queryConfig: QueryConfig): MutableMap<Int, DataUsage> {
        return DataUsageUtil.findAppDataUsage(this, queryConfig)
    }

    private fun getDeviceUsage(queryConfig: QueryConfig): DataUsage {
        return DataUsageUtil.findDeviceDataUsage(this, queryConfig)
    }

    private fun getCollectionInterval(queryConfig: QueryConfig): Long {
        var interval = queryConfig.collectionInterval
        val tp = queryConfig.timePeriod
        if (System.currentTimeMillis() - tp.endTime > interval) {
            /*
            somehow, service stopped and restarted after few min or hour. collect data from last end time to current time
             */
            interval = 0
        }
        if ((tp.endTime - tp.startTime) + interval > TimeUnit.HOURS.toMillis(2)) {
            interval = TimeUnit.HOURS.toMillis(2) - (tp.endTime - tp.startTime)
        }
        return interval
    }

    private fun updateAppUsageDb(queryConfig: QueryConfig, usageMap: Map<Int, DataUsage>) {
        Log.d(TAG, "updateDb: " + usageMap.size)
        val prevUsageMap = RoomDB.getDatabase().appDataUsageDao()
            .getByTime(queryConfig.timePeriod.startTime, queryConfig.prevCollectionEndTime)
            .groupBy { it.appUid }
            .mapValues {
                DataUsage(
                    it.value.sumOf { it.appTx },
                    it.value.sumOf { it.appRx },
                    it.value.sumOf { it.txPackets },
                    it.value.sumOf { it.rxPackets })
            }

        val usageList = ArrayList<AppDataUsage>()
        for (usage in usageMap) {
            if (prevUsageMap.containsKey(usage.key)) {
                usage.value.txBytes -= prevUsageMap[usage.key]!!.txBytes
                usage.value.rxBytes -= prevUsageMap[usage.key]!!.rxBytes
                usage.value.txPackets -= prevUsageMap[usage.key]!!.txPackets
                usage.value.rxPackets -= prevUsageMap[usage.key]!!.rxPackets
            }
            if (usage.value.txBytes + usage.value.rxBytes > 0) {
                usageList.add(
                    AppDataUsage(
                        0,
                        usage.key,
                        Util.uidToPackageName(this, usage.key),
                        queryConfig.networkType,
                        queryConfig.prevCollectionEndTime,
                        queryConfig.timePeriod.endTime,
                        usage.value.txBytes,
                        usage.value.rxBytes,
                        usage.value.txPackets,
                        usage.value.rxPackets,
                        Util.getDateDefault(queryConfig.prevCollectionEndTime),
                        Util.getDateDefault(queryConfig.timePeriod.endTime),
                        Util.getFileSize(usage.value.txBytes),
                        Util.getFileSize(usage.value.rxBytes)
                    )
                )
            }
        }
        Log.d(TAG, "updateDb:usageList " + usageList.size)
        RoomDB.getDatabase().appDataUsageDao().insertAll(usageList)
        Log.d(
            TAG,
            "app usage total: st: ${Util.getDateDefault(queryConfig.prevCollectionEndTime)}, et: ${
                Util.getDateDefault(queryConfig.timePeriod.endTime)
            }, tx ${usageList.sumOf { it.appTx }} ,rx: ${usageList.sumOf { it.appRx }}, txPac ${
                usageList.sumOf { it.txPackets }
            } ,rxPac: ${
                usageList.sumOf { it.rxPackets }
            }, tx ${Util.getFileSize(usageList.sumOf { it.appTx })} ,rx: ${
                Util.getFileSize(
                    usageList.sumOf { it.appRx })
            }"
        )
    }

    private fun updateDeviceUsageDb(queryConfig: QueryConfig, dataUsage: DataUsage) {
        val prevUsageMap = RoomDB.getDatabase().deviceDataUsageDao()
            .getByTime(queryConfig.timePeriod.startTime, queryConfig.prevCollectionEndTime)

        if (prevUsageMap.isNotEmpty()) {
            dataUsage.txBytes -= prevUsageMap.sumOf { it.txBytes }
            dataUsage.rxBytes -= prevUsageMap.sumOf { it.rxBytes }
            dataUsage.txPackets -= prevUsageMap.sumOf { it.txPackets }
            dataUsage.rxPackets -= prevUsageMap.sumOf { it.rxPackets }
        }
        if (dataUsage.txBytes + dataUsage.rxBytes > 0) {
            RoomDB.getDatabase().deviceDataUsageDao().insert(
                DeviceDataUsage(
                    id = 0,
                    networkType = queryConfig.networkType,
                    startTime = queryConfig.prevCollectionEndTime,
                    endTime = queryConfig.timePeriod.endTime,
                    txBytes = dataUsage.txBytes,
                    rxBytes = dataUsage.rxBytes,
                    txPackets = dataUsage.txPackets,
                    rxPackets = dataUsage.rxPackets,
                    rStartTime = Util.getDateDefault(queryConfig.prevCollectionEndTime),
                    rEndTime = Util.getDateDefault(queryConfig.timePeriod.endTime),
                    rTx = Util.getFileSize(dataUsage.txBytes),
                    rRx = Util.getFileSize(dataUsage.rxBytes)
                )
            )
        }
        Log.d(
            TAG,
            "device usage total: st: ${Util.getDateDefault(queryConfig.prevCollectionEndTime)}, et: ${
                Util.getDateDefault(queryConfig.timePeriod.endTime)
            }, tx ${dataUsage.txBytes} ,rx: ${dataUsage.rxBytes}, txPac ${dataUsage.txPackets} ,rxPac: ${
                dataUsage.rxPackets
            }, tx ${Util.getFileSize(dataUsage.txBytes)} ,rx: ${Util.getFileSize(dataUsage.rxBytes)}"
        )
    }
}