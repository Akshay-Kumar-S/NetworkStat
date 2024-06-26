package com.test.networkstat.utils

import android.content.Context
import android.net.NetworkCapabilities
import android.util.Log
import com.test.networkstat.database.models.QueryConfig
import com.test.networkstat.database.models.TimePeriod

class TestUtil(var timePeriod: TimePeriod) {
    private var TAG = "akshay"
    private var totalAppUsage = 0L
    private var totalDeviceUsage = 0L

    fun getAppDataUsageFromStartup(context: Context) {
        val queryConfig = QueryConfig(NetworkCapabilities.TRANSPORT_WIFI)
        queryConfig.timePeriod = timePeriod
        val usages = DataUsageUtil.findAppDataUsage(context, queryConfig)
        var totalTx = 0L
        var totalRx = 0L
        var totalTxPac = 0L
        var totalRxPac = 0L
        for (usage in usages) {
            totalTx += usage.value.txBytes
            totalRx += usage.value.rxBytes
            totalTxPac += usage.value.txPackets
            totalRxPac += usage.value.rxPackets
        }
        totalAppUsage = totalTx + totalRx + totalTxPac + totalRxPac
        Log.d(TAG, "getAppDataUsageFromStartup: totalTx $totalTx")
        Log.d(TAG, "getAppDataUsageFromStartup: totalRx $totalRx")
        Log.d(TAG, "getAppDataUsageFromStartup: totalTxPac $totalTxPac")
        Log.d(TAG, "getAppDataUsageFromStartup: totalRxPac $totalRxPac")
        Log.d(
            TAG,
            "getAppDataUsageFromStartup: total apps Usage $totalAppUsage [${
                Util.getFileSize(totalAppUsage)
            }]"
        )
    }

    fun getDeviceDataUsageFromStartup(context: Context) {
        val queryConfig = QueryConfig(NetworkCapabilities.TRANSPORT_WIFI)
        queryConfig.timePeriod = timePeriod
        val usage = DataUsageUtil.findDeviceDataUsage(context, queryConfig)
        totalDeviceUsage = usage.txBytes + usage.rxBytes + usage.txPackets + usage.rxPackets
        Log.d(TAG, "getDeviceDataUsageFromStartup: totalTx ${usage.txBytes}")
        Log.d(TAG, "getDeviceDataUsageFromStartup: totalRx ${usage.rxBytes}")
        Log.d(TAG, "getDeviceDataUsageFromStartup: totalTxPac ${usage.txPackets}")
        Log.d(TAG, "getDeviceDataUsageFromStartup: totalRxPac ${usage.rxPackets}")
        Log.d(
            TAG,
            "getDeviceDataUsageFromStartup: total device Usage $totalDeviceUsage [${
                Util.getFileSize(totalDeviceUsage)
            }]"
        )
    }

    fun printAppDeviceUsageDifferent() {
        val diff = totalDeviceUsage - totalAppUsage
        Log.d(TAG, "printAppDeviceUsageDifferent: $diff [${Util.getFileSize(diff)}]")
    }
}