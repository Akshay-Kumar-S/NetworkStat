package com.test.networkstat.utils

import android.app.usage.NetworkStats
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.test.networkstat.App
import com.test.networkstat.database.models.TimePeriod
import com.test.networkstat.managers.PrefManager
import com.test.networkstat.services.AppService
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Util {
    private var TAG = "akshay"

    fun getUid(appName: String): Int {
        var uid = 0
        try {
            uid = App.getInstance().packageManager.getApplicationInfo(appName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return uid
    }

    fun getDateDefault(milliSeconds: Long): String {
        return getDate(milliSeconds, "dd/MM/yyyy HH:mm:ss.SSS")
    }

    fun getDate(milliSeconds: Long, format: String): String {
        // Create a DateFormatter object for displaying date in specified format.
        //"dd/MM/yyyy hh:mm:ss.SSS"
        val formatter = SimpleDateFormat(format)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }

    fun getStartTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 16)
        date.set(Calendar.MINUTE, 30)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)
        return date.timeInMillis
    }

    fun getEndTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 18)
        date.set(Calendar.MINUTE, 15)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)
        return date.timeInMillis
    }

    fun getSimSubscriberId(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                return null
            val tm =
                App.getInstance().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.subscriberId
        } catch (e: Exception) {
        }
        return ""
    }

    fun logTime(param: String, time: Long) {
        Log.d(TAG, param + ": " + getDate(time, "dd/MM/yyyy HH:mm:ss.SSS"))
    }

    fun updateLastCollectionTime(ctx: Context, timePeriod: TimePeriod) {
        setLastCollectionStartTime(ctx, getLastCollectionEndTime(ctx))
        setLastCollectionEndTime(ctx, timePeriod.endTime)
    }

    fun getLastCollectionEndTime(ctx: Context): Long {
        var time = PrefManager.createInstance(ctx).getLong(PrefManager.LAST_COLLECTION_END_TIME, 0L)
        if (time == 0L) {
            time = DataUsageUtil.getLastAggregatedTime()
        }
        return time
    }

    private fun setLastCollectionStartTime(ctx: Context, time: Long) {
        PrefManager.createInstance(ctx).putLong(PrefManager.LAST_COLLECTION_START_TIME, time)
    }

    fun setLastCollectionEndTime(ctx: Context, time: Long) {
        PrefManager.createInstance(ctx).putLong(PrefManager.LAST_COLLECTION_END_TIME, time)
    }

    fun uidToPackageName(ctx: Context, uid: Int): String {
        val packageName = ctx.packageManager.getNameForUid(uid)
            ?: when (uid) {
                NetworkStats.Bucket.UID_REMOVED -> "removed"
                NetworkStats.Bucket.UID_TETHERING -> "tethering"
                else -> "unknown"
            }

        return if (':' in packageName)
            packageName.substring(0, packageName.indexOf(':'))
        else
            packageName
    }

    fun startService(context: Context) {
        Intent(context, AppService::class.java).also { intent ->
            context.startService(intent)
        }
    }

    fun stopService(context: Context) {
        Intent(context, AppService::class.java).also { intent ->
            context.stopService(intent)
        }
    }

    private fun checkUId(ctx: Context) {
        val appInfoList = ctx.packageManager.getInstalledPackages(0)
        for (appInfo in appInfoList) {
            if (appInfo.applicationInfo.uid == 1051) {
                Log.d(TAG, "checkUId:1051 " + appInfo.packageName)
            } else if (appInfo.applicationInfo.uid == 1052) {
                Log.d(TAG, "checkUId:1052 " + appInfo.packageName)
            } else if (appInfo.applicationInfo.uid == 0) {
                Log.d(TAG, "checkUId:0 " + appInfo.packageName)
            }
        }
    }
}