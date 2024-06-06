package com.test.networkstat.utils

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.test.networkstat.App
import com.test.networkstat.database.models.DataUsage
import com.test.networkstat.database.models.QueryConfig
import com.test.networkstat.database.models.TimePeriod
import com.test.networkstat.managers.PrefManager
import java.util.*
import java.util.concurrent.TimeUnit

object DataUsageUtil {
    private var TAG = "akshay"

    private fun findAggregationTime(): Long {
        Log.d(TAG, "findAggregationTime: ")
        val nsm = App.getInstance()
            .getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        var aggregationTime: Long = -1
        for (networkType in 0..1) {
            val networkStats = nsm.queryDetailsForUid(
                networkType,
                Util.getSimSubscriberId(),
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(105),
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2),
                Util.getUid("com.android.chrome")
            )
            val bucket = NetworkStats.Bucket()
            if (networkStats.hasNextBucket()) {
                Log.d(TAG, "findAggregationTime: " + Util.getDateDefault(bucket.startTimeStamp))
                networkStats.getNextBucket(bucket)
                aggregationTime = bucket.startTimeStamp
                networkStats.close()
                break
            }
        }
        return aggregationTime
    }

    fun getBucketTime(time: Long = System.currentTimeMillis()): TimePeriod {
        Log.d(TAG, "getLastAggregatedTime: ")
        val aggregationTime = getAggregationTime()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        if (aggregationTime != -1L) {
            val aCalendar = Calendar.getInstance()
            aCalendar.timeInMillis = aggregationTime

            calendar.set(Calendar.MINUTE, aCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, aCalendar.get(Calendar.SECOND))
            calendar.set(Calendar.MILLISECOND, aCalendar.get(Calendar.MILLISECOND))

            val aHourEven = isEven(aCalendar.get(Calendar.HOUR_OF_DAY))
            val cHourEven = isEven(calendar.get(Calendar.HOUR_OF_DAY))

            if ((aHourEven && cHourEven) || (!aHourEven && !cHourEven)) {
                if (calendar.timeInMillis > time) {
                    calendar.timeInMillis -= TimeUnit.HOURS.toMillis(2)
                }
            } else {
                calendar.timeInMillis -= TimeUnit.HOURS.toMillis(1)
            }
        } else {
            Log.d(TAG, "getLastAggregatedTime: else ")
            //TODO process aggregationTime = -1
        }
        return TimePeriod(calendar.timeInMillis, calendar.timeInMillis + TimeUnit.HOURS.toMillis(2))
    }

    private fun getAggregationTime(ctx: Context = App.getInstance()): Long {
        Log.d(TAG, "getAggregationTime: ")
        var time = PrefManager.createInstance(ctx).getLong(PrefManager.AGGREGATION_TIME, -1)
        if (time == -1L) {
            time = findAggregationTime()
            setAggregationTime(ctx, time)
        }
        return time
    }

    fun getStartEndTime(prevCollectionEndTime: Long): TimePeriod {
        val timePeriod = getBucketTime()
        return if (prevCollectionEndTime < timePeriod.startTime) {
            getBucketTime(prevCollectionEndTime)
        } else {
            timePeriod.endTime = System.currentTimeMillis()
            timePeriod
        }
    }

    private fun setAggregationTime(ctx: Context, value: Long) {
        PrefManager.createInstance(ctx).putLong(PrefManager.AGGREGATION_TIME, value)
    }

    private fun isEven(value: Int): Boolean {
        return value % 2 == 0
    }

    fun findAppDataUsage(ctx: Context, queryConfig: QueryConfig): MutableMap<Int, DataUsage> {
        Log.e(TAG, "findAppDataUsage: " + queryConfig.networkType)
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        val appUsageMap = mutableMapOf<Int, DataUsage>()
        try {
            networkStats = networkStatsManager.querySummary(
                queryConfig.networkType,
                Util.getSimSubscriberId(),
                queryConfig.timePeriod.startTime,
                queryConfig.timePeriod.endTime
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                if (appUsageMap.containsKey(bucket.uid)) {
                    appUsageMap[bucket.uid]!!.txBytes += bucket.txBytes
                    appUsageMap[bucket.uid]!!.rxBytes += bucket.rxBytes
                    appUsageMap[bucket.uid]!!.txPackets += bucket.txPackets
                    appUsageMap[bucket.uid]!!.rxPackets += bucket.rxPackets
                } else {
                    logBucket(bucket, 10250)
                    appUsageMap[bucket.uid] = DataUsage(
                        bucket.txBytes,
                        bucket.rxBytes,
                        bucket.txPackets,
                        bucket.rxPackets
                    )
                }
            }
            networkStats.close()
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        logAppUsage(appUsageMap[10250])
        return appUsageMap
    }

    fun findAppUsageQueryDetailsForUid(
        ctx: Context,
        queryConfig: QueryConfig
    ): MutableMap<Int, DataUsage> {
        Log.e(TAG, "findAppUsageQueryDetailsForUid: " + queryConfig.networkType)
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val appUsageMap = mutableMapOf<Int, DataUsage>()
        try {
            val uidMap = Util.findSharedUid(ctx)
            for ((uid, apps) in uidMap) {
                val networkStats = networkStatsManager.queryDetailsForUid(
                    queryConfig.networkType,
                    Util.getSimSubscriberId(),
                    queryConfig.timePeriod.startTime,
                    queryConfig.timePeriod.endTime,
                    uid
                )
                val bucket = NetworkStats.Bucket()
                while (networkStats.hasNextBucket()) {
                    networkStats.getNextBucket(bucket)
                    if (appUsageMap.containsKey(bucket.uid)) {
                        appUsageMap[bucket.uid]!!.txBytes += bucket.txBytes
                        appUsageMap[bucket.uid]!!.rxBytes += bucket.rxBytes
                        appUsageMap[bucket.uid]!!.txPackets += bucket.txPackets
                        appUsageMap[bucket.uid]!!.rxPackets += bucket.rxPackets
                    } else {
                        logBucket(bucket, 10250)
                        appUsageMap[bucket.uid] = DataUsage(
                            bucket.txBytes,
                            bucket.rxBytes,
                            bucket.txPackets,
                            bucket.rxPackets
                        )
                    }
                    if (apps.size > 1 && appUsageMap.containsKey(bucket.uid)) {
                        appUsageMap[bucket.uid]!!.txBytes *= apps.size
                        appUsageMap[bucket.uid]!!.rxBytes *= apps.size
                        appUsageMap[bucket.uid]!!.txPackets *= apps.size
                        appUsageMap[bucket.uid]!!.rxPackets *= apps.size
                    }
                }
                networkStats.close()
            }
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        logAppUsage(appUsageMap[10250])
        return appUsageMap
    }

    private fun logBucket(bucket: NetworkStats.Bucket, uid: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bucket.uid == uid) {
            Log.i(
                TAG,
                "uid: ${bucket.uid}, " +
                        "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                        "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " /* +
                        "tx Bytes: ${bucket.txBytes},  " +
                        "rx Bytes: ${bucket.rxBytes},  " +
                        "tx Packet: ${bucket.txPackets},  " +
                        "rx Packet: ${bucket.rxPackets},  " +
                        "state: ${getReadableState(bucket.state)},  " +
                        "roaming: ${getReadableRoaming(bucket.roaming)},  " +
                        "metered: ${getReadableMetered(bucket.metered)},  " +
                        "tag: ${bucket.tag},  " +
                        "defaultNetwork: ${
                            getReadableDefaultNetwork(
                                bucket.defaultNetworkStatus
                            )
                        },  "*/
            )
        }
    }

    private fun logAppUsage(dataUsage: DataUsage?) {
        if (dataUsage == null) return
        Log.i(TAG, "logAppUsage:tx " + dataUsage.txBytes)
        Log.i(TAG, "logAppUsage:rx " + dataUsage.rxBytes)
        Log.i(TAG, "logAppUsage:txp " + dataUsage.txPackets)
        Log.i(TAG, "logAppUsage:rxp " + dataUsage.rxPackets)
        val totalUsage =
            dataUsage.txBytes + dataUsage.rxBytes + dataUsage.txPackets + dataUsage.rxPackets
        Log.i(TAG, "logAppUsage:tot $totalUsage [${Util.getFileSize(totalUsage)}]")
    }

    fun findDeviceDataUsage(ctx: Context, queryConfig: QueryConfig): DataUsage {
        Log.e(TAG, "findDeviceDataUsage: " + queryConfig.networkType)
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val bucket: NetworkStats.Bucket = networkStatsManager.querySummaryForDevice(
            queryConfig.networkType,
            Util.getSimSubscriberId(),
            queryConfig.timePeriod.startTime,
            queryConfig.timePeriod.endTime
        )
        Log.d(
            TAG,
            "findDeviceDataUsage: st: ${Util.getDateDefault(bucket.startTimeStamp)}, et: ${
                Util.getDateDefault(bucket.endTimeStamp)
            }"
        )
        return DataUsage(bucket.txBytes, bucket.rxBytes, bucket.txPackets, bucket.rxPackets)
    }

    fun findDeviceUsageQueryDetails(ctx: Context, queryConfig: QueryConfig): DataUsage {
        Log.e(TAG, "findDeviceUsageQueryDetails: " + queryConfig.networkType)
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val ns: NetworkStats = networkStatsManager.queryDetails(
            queryConfig.networkType,
            Util.getSimSubscriberId(),
            queryConfig.timePeriod.startTime,
            queryConfig.timePeriod.endTime
        )
        val dataUsage = DataUsage(0, 0, 0, 0)
        do {
            val bucket: NetworkStats.Bucket = NetworkStats.Bucket()
            Log.d(
                TAG,
                "findDeviceUsageQueryDetails st: ${Util.getDateDefault(bucket.startTimeStamp)}, et: ${
                    Util.getDateDefault(bucket.endTimeStamp)
                } "
            )
            ns.getNextBucket(bucket)
            dataUsage.txBytes += bucket.txBytes
            dataUsage.rxBytes += bucket.rxBytes
            dataUsage.txPackets += bucket.txPackets
            dataUsage.rxPackets += bucket.rxPackets
        } while ((ns.hasNextBucket()))
        return dataUsage
    }

    private fun getReadableState(state: Int): String {
        return when (state) {
            NetworkStats.Bucket.STATE_ALL -> "STATE_ALL"
            NetworkStats.Bucket.STATE_DEFAULT -> "STATE_DEFAULT"
            NetworkStats.Bucket.STATE_FOREGROUND -> "STATE_FOREGROUND"
            else -> "Unknown"
        }
    }

    private fun getReadableRoaming(roaming: Int): String {
        return when (roaming) {
            NetworkStats.Bucket.ROAMING_ALL -> "ROAMING_ALL"
            NetworkStats.Bucket.ROAMING_NO -> "ROAMING_NO"
            NetworkStats.Bucket.ROAMING_YES -> "ROAMING_YES"
            else -> "Unknown"
        }
    }

    private fun getReadableMetered(metered: Int): String {
        return when (metered) {
            NetworkStats.Bucket.METERED_ALL -> "METERED_ALL"
            NetworkStats.Bucket.METERED_NO -> "METERED_NO"
            NetworkStats.Bucket.METERED_YES -> "METERED_YES"
            else -> "Unknown"
        }
    }

    private fun getReadableDefaultNetwork(network: Int): String {
        return when (network) {
            NetworkStats.Bucket.DEFAULT_NETWORK_ALL -> "DEFAULT_ALL"
            NetworkStats.Bucket.DEFAULT_NETWORK_NO -> "DEFAULT_NO"
            NetworkStats.Bucket.DEFAULT_NETWORK_YES -> "DEFAULT_YES"
            else -> "Unknown"
        }
    }
}