package com.test.networkstat.utils

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkCapabilities
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.test.networkstat.App
import com.test.networkstat.database.RoomDB
import com.test.networkstat.database.models.AppUsage
import com.test.networkstat.database.models.QueryConfig
import com.test.networkstat.database.models.TimePeriod
import com.test.networkstat.managers.PrefManager
import java.util.*
import java.util.concurrent.TimeUnit

object DataUsageUtil {
    private var TAG = "akshay"

    private fun findAggregationTime(): Long {
        val nsm = App.getInstance()
            .getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        var aggregationTime: Long = -1
        for (networkType in 0..1) {
            val networkStats = nsm.queryDetailsForUid(
                networkType,
                Util.getSimSubscriberId(),
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(105),
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2),
                Util.getUid("com.google.android.youtube")
            )
            val bucket = NetworkStats.Bucket()
            if (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                aggregationTime = bucket.startTimeStamp
                networkStats.close()
                break
            }
        }
        return aggregationTime
    }

    fun getLastAggregatedTime(): Long {
        val aggregationTime = getAggregationTime()
        val currentTime = Calendar.getInstance()
        if (aggregationTime != -1L) {
            val aCalendar = Calendar.getInstance()
            aCalendar.timeInMillis = aggregationTime

            currentTime.set(Calendar.MINUTE, aCalendar.get(Calendar.MINUTE))
            currentTime.set(Calendar.SECOND, aCalendar.get(Calendar.SECOND))
            currentTime.set(Calendar.MILLISECOND, aCalendar.get(Calendar.MILLISECOND))

            val aHourEven = isEven(aCalendar.get(Calendar.HOUR_OF_DAY))
            val cHourEven = isEven(currentTime.get(Calendar.HOUR_OF_DAY))

            if ((aHourEven && cHourEven) || (!aHourEven && !cHourEven)) {
                if (currentTime.timeInMillis > System.currentTimeMillis()) {
                    currentTime.timeInMillis -= TimeUnit.HOURS.toMillis(2)
                }
            } else {
                currentTime.timeInMillis -= TimeUnit.HOURS.toMillis(1)
            }
        } else {
            Log.d(TAG, "getLastAggregatedTime: else ")
            //TODO process aggregationTime = -1
        }
        return currentTime.timeInMillis
    }

    private fun getAggregationTime(ctx: Context = App.getInstance()): Long {
        var time = PrefManager.createInstance(ctx).getLong(PrefManager.AGGREGATION_TIME, -1)
        if (time == -1L) {
            time = findAggregationTime()
        }
        return time
    }

    private fun isEven(value: Int): Boolean {
        return value % 2 == 0
    }

    fun findAppDataUsage(ctx: Context, queryConfig: QueryConfig): MutableMap<Int, AppUsage> {
        Log.e(TAG, "findAppDataUsage: ")
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        val appUsageMap = mutableMapOf<Int, AppUsage>()
        try {
            val uid = Util.getUid("com.google.android.youtube")
            networkStats = networkStatsManager.querySummary(
                queryConfig.networkType,
                Util.getSimSubscriberId(),
                queryConfig.timePeriod.startTime,
                queryConfig.timePeriod.endTime
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                logBucket(bucket, uid)
                if (appUsageMap.containsKey(bucket.uid)) {
                    appUsageMap[bucket.uid]!!.txBytes += bucket.txBytes
                    appUsageMap[bucket.uid]!!.rxBytes += bucket.rxBytes
                } else {
                    appUsageMap[bucket.uid] = AppUsage(bucket.uid, bucket.txBytes, bucket.rxBytes)
                }
            }
            networkStats.close()
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        return appUsageMap
    }

    fun logTotalAppUsage() {
        Log.d(TAG, "logTotalAppUsage: ")
        val appUsage = RoomDB.getDatabase().appDataUsageDao().getAll()
        Log.d(
            TAG,
            "st: ${Util.getDateDefault(appUsage.first().startTime)}, " +
                    "et: ${Util.getDateDefault(appUsage.last().endTime)},  " +
                    "tx Bytes: ${appUsage.sumOf { it.appTx }},  " +
                    "rx Bytes: ${appUsage.sumOf { it.appRx }},  "
        )
    }

    fun logDeviceUsage() {
        try {
            findDeviceDataUsage(
                App.getInstance(),
                QueryConfig(
                    NetworkCapabilities.TRANSPORT_CELLULAR,
                    TimePeriod(
                        RoomDB.getDatabase().appDataUsageDao().getFirstRow().startTime,
                        Util.getLastCollectionEndTime(App.getInstance())
                    )
                )
            )
        } catch (e: Exception) {
            Log.d(TAG, "logDeviceUsage: ", e)
        }
    }

    private fun findDeviceDataUsage(ctx: Context, queryConfig: QueryConfig) {
        Log.d(TAG, "findDeviceDataUsage: ")
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
            "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                    "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " +
                    "tx Bytes: ${bucket.txBytes},  " +
                    "rx Bytes: ${bucket.rxBytes},  "
        )
        /*Log.d(
            TAG,
            "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                    "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " +
                    "tx Bytes: ${Util.getFileSize(bucket.txBytes)},  " +
                    "rx Bytes: ${Util.getFileSize(bucket.rxBytes)},  " +
                    "tx Packet: ${Util.getFileSize(bucket.txPackets)},  " +
                    "rx Packet: ${Util.getFileSize(bucket.rxPackets)},  "
        )*/
    }

    private fun logBucket(bucket: NetworkStats.Bucket, uid: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bucket.uid == uid) {
            Log.d(
                TAG,
                "uid: ${bucket.uid}, " +
                        "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                        "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " +
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
                        },  "
            )
        }
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