package com.test.networkstat

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var TAG = "akshay"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!getUsageAccessPermission()) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            } else {
                val startTime = 1605779115000
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "getUsage:startTime  " + getDate(startTime, "dd/MM/yyyy hh:mm:ss.SSS"))
                Log.d(TAG, "getUsage:endTime  " + getDate(endTime, "dd/MM/yyyy hh:mm:ss.SSS"))
                Log.e(TAG, "onCreate: device usage")
                getDeviceUsage(startTime, endTime)
                Log.e(TAG, "onCreate: app usage")
                getAppUsage(startTime, endTime)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getAppUsage(startTime: Long, endTime: Long): Long {
        val uid = getUid("com.google.android.youtube")
        val networkStatsManager = applicationContext.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
        val networkStats: NetworkStats
        var totalUsage = 0L
        try {
            networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime)

            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                if (bucket.uid == uid) {
                    totalUsage += bucket.txBytes + bucket.rxBytes
                }
            }
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        Log.d(TAG, "getUsage: " + getFileSize(totalUsage))
        return totalUsage
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getDeviceUsage(startTime: Long, endTime: Long): Long {
        val networkStatsManager = applicationContext.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
        val bucket: NetworkStats.Bucket
        var totalUsage = 0L
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime)
            totalUsage += bucket.txBytes + bucket.rxBytes
        } catch (e: RemoteException) {
            Log.d(TAG, "getUsage: RemoteException")
        }
        Log.d(TAG, "getUsage: " + getFileSize(totalUsage))
        return totalUsage
    }

    private fun getUid(appName: String): Int {
        var uid = 0
        try {
            uid = this.packageManager.getApplicationInfo(appName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return uid
    }

    private fun getDate(milliSeconds: Long, format: String): String? {
        // Create a DateFormatter object for displaying date in specified format.
        //"dd/MM/yyyy hh:mm:ss.SSS"
        val formatter = SimpleDateFormat(format)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    private fun getFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())).toString() + " " + units[digitGroups]
    }
}