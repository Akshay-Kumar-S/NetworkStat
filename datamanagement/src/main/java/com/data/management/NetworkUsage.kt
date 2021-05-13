package com.data.management

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkCapabilities
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.TimeUnit

class NetworkUsage(
    private val ctx: Context, private val startTime: Long, private val endTime: Long
) {
    private var TAG = "akshay"

    fun getAppDataUsage(): Map<Int, AppUsage> {
        return if (isBelow120Min()) {
            Log.d(TAG, "getAppDataUsage: < 120 min")
            val beginTime = startTime - TimeUnit.HOURS.toMillis(2)
            val firstUsage = findAppDataUsage(beginTime, startTime)
            val secondUsage = findAppDataUsage(beginTime, endTime)
            calculateIntervalUsage(firstUsage, secondUsage)
        } else {
            findAppDataUsage(startTime, endTime)
        }
    }

    private fun isBelow120Min(): Boolean {
        return (endTime - startTime) < TimeUnit.MINUTES.toMillis(120)
    }

    private fun isEndTime2HrBehind(): Boolean {
        return System.currentTimeMillis() - endTime > TimeUnit.MINUTES.toMillis(120)
    }

    private fun findAppDataUsage(startTime: Long, endTime: Long): MutableMap<Int, AppUsage> {
        Log.e(TAG, "findAppDataUsage: ")
        //findDeviceDataUsage(startTime, endTime)
        getUidTxRxBytes(10069)
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        val appUsageMap = mutableMapOf<Int, AppUsage>()
        try {
            val uid = Util.getUid(ctx, "com.google.android.youtube")
            networkStats = networkStatsManager.querySummary(
                NetworkCapabilities.TRANSPORT_CELLULAR,
                Util.getSimSubscriberId(ctx),
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
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
                                "state: ${Util.getReadableState(bucket.state)},  " +
                                "roaming: ${Util.getReadableRoaming(bucket.roaming)},  " +
                                "metered: ${Util.getReadableMetered(bucket.metered)},  " +
                                "tag: ${bucket.tag},  " +
                                "defaultNetwork: ${Util.getReadableDefaultNetwork(bucket.defaultNetworkStatus)},  "
                    )
                }
                if (appUsageMap.containsKey(bucket.uid)) {
                    appUsageMap[bucket.uid]!!.txBytes += bucket.txBytes
                    appUsageMap[bucket.uid]!!.rxBytes += bucket.rxBytes
                } else {
                    appUsageMap[bucket.uid] = AppUsage(bucket.txBytes, bucket.rxBytes)
                }
            }
            networkStats.close()
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        return appUsageMap
    }

    private fun findDeviceDataUsage(startTime: Long, endTime: Long) {
        Log.d(TAG, "findDeviceDataUsage: ")
        val networkStatsManager =
            ctx.getSystemService(AppCompatActivity.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val bucket: NetworkStats.Bucket = networkStatsManager.querySummaryForDevice(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            Util.getSimSubscriberId(ctx),
            startTime,
            endTime
        )
        Log.d(
            TAG,
            "st: ${Util.getDateDefault(bucket.startTimeStamp)}, " +
                    "et: ${Util.getDateDefault(bucket.endTimeStamp)},  " +
                    "tx Bytes: ${Util.getFileSize(bucket.txBytes)},  " +
                    "rx Bytes: ${Util.getFileSize(bucket.rxBytes)},  " +
                    "tx Packet: ${Util.getFileSize(bucket.txPackets)},  " +
                    "rx Packet: ${Util.getFileSize(bucket.rxPackets)},  "
        )
    }

    private fun getUidTxRxBytes(appUid: Int = 10069) {
        Log.e(TAG, "getUidTxRxBytes: ")
        var txBytes: Long = 0
        var rxBytes: Long = 0
        val dir = File("/proc/uid/")
        if (dir.exists()) {
            val childDir = dir.list()
            if (!Arrays.asList(*childDir).contains(appUid.toString())) {
                return
            }
            val uidUsageDir = File("/proc/uid/$appUid")
            val uidDataTxPath = File(uidUsageDir, "tcp_snd")
            val uidDataRxPath = File(uidUsageDir, "tcp_rcv")
            try {
                val brSent = BufferedReader(FileReader(uidDataTxPath))
                val brReceived = BufferedReader(FileReader(uidDataRxPath))
                var sentBytes: String?
                var receivedBytes: String?
                if (brSent.readLine().also { sentBytes = it } != null) {
                    txBytes = java.lang.Long.valueOf(sentBytes)
                }
                if (brReceived.readLine().also { receivedBytes = it } != null) {
                    rxBytes = java.lang.Long.valueOf(receivedBytes)
                }
                brSent.close()
                brReceived.close()
                Log.d(TAG, "getUidTxRxBytes: " + txBytes)
                Log.d(TAG, "getUidTxRxBytes: " + rxBytes)
            } catch (e: Exception) {
                //IOException or FileNotFoundException
            }
        }
    }

    private fun calculateIntervalUsage(
        firstUsage: MutableMap<Int, AppUsage>, secondUsage: MutableMap<Int, AppUsage>
    ): Map<Int, AppUsage> {
        Log.d(TAG, "calculateIntervalUsage: ")
        for (usageMap in secondUsage) {
            if (firstUsage.containsKey(usageMap.key)) {
                usageMap.value.txBytes = usageMap.value.txBytes - firstUsage[usageMap.key]!!.txBytes
                usageMap.value.rxBytes = usageMap.value.rxBytes - firstUsage[usageMap.key]!!.rxBytes
                Log.d(
                    TAG,
                    "calculateIntervalUsage: " + (usageMap.value.txBytes + usageMap.value.rxBytes)
                )
            }
        }
        return secondUsage
    }
}