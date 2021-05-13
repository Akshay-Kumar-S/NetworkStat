package com.test.networkstat.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import com.test.networkstat.database.RoomDB
import com.test.networkstat.database.entities.AppDataUsage
import com.test.networkstat.database.models.AppUsage
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
            val notification: Notification = Notification.Builder(this, "soti_service")
                .setContentTitle("SOTI")
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
                val queryConfig = QueryConfig(
                    NetworkCapabilities.TRANSPORT_CELLULAR,
                    TimePeriod(DataUsageUtil.getLastAggregatedTime(), System.currentTimeMillis())
                )
                val appUsageMap = DataUsageUtil.findAppDataUsage(this, queryConfig)
                updateDb(appUsageMap, queryConfig)

                Util.updateLastCollectionTime(this, queryConfig.timePeriod)
                DataUsageUtil.logDeviceUsage()
                DataUsageUtil.logTotalAppUsage()
                try {
                    Thread.sleep(getCollectionInterval(queryConfig.timePeriod))
                } catch (e: InterruptedException) {
                    Log.d(TAG, "startThread: ", e)
                }
            } while (running)
            stopSelf()
        }
        t!!.start()
    }

    private fun getCollectionInterval(timePeriod: TimePeriod): Long {
        var interval = TimeUnit.MINUTES.toMillis(2)
        if ((timePeriod.endTime - timePeriod.startTime) + interval > TimeUnit.HOURS.toMillis(2)) {
            interval = TimeUnit.HOURS.toMillis(2) - (timePeriod.endTime - timePeriod.startTime)
        }
        return interval
    }

    private fun updateDb(usageMap: Map<Int, AppUsage>, queryConfig: QueryConfig) {
        Log.d(TAG, "updateDb: " + usageMap.size)
        val prevCollectionEndTime = Util.getLastCollectionEndTime(this)
        val prevUsageMap = RoomDB.getDatabase().appDataUsageDao()
            .getByTime(queryConfig.timePeriod.startTime, prevCollectionEndTime)
            .groupBy { it.appUid }
            .mapValues { Pair(it.value.sumOf { it.appTx }, it.value.sumOf { it.appRx }) }

        val usageList = ArrayList<AppDataUsage>()
        for (usage in usageMap) {
            if (prevUsageMap.containsKey(usage.key)) {
                usage.value.txBytes -= prevUsageMap[usage.key]!!.first
                usage.value.rxBytes -= prevUsageMap[usage.key]!!.second
            }
            if (usage.value.txBytes + usage.value.rxBytes > 0) {
                usageList.add(
                    AppDataUsage(
                        0,
                        usage.key,
                        Util.uidToPackageName(this, usage.key),
                        queryConfig.networkType,
                        prevCollectionEndTime,
                        queryConfig.timePeriod.endTime,
                        usage.value.txBytes,
                        usage.value.rxBytes,
                        Util.getDateDefault(prevCollectionEndTime),
                        Util.getDateDefault(queryConfig.timePeriod.endTime),
                        Util.getFileSize(usage.value.txBytes),
                        Util.getFileSize(usage.value.rxBytes)
                    )
                )
            }
        }
        Log.d(TAG, "updateDb:usageList " + usageList.size)
        RoomDB.getDatabase().appDataUsageDao().insertAll(usageList)
    }
}