package com.test.networkstat.utils

import android.app.usage.NetworkStats
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.test.networkstat.App
import com.test.networkstat.database.RoomDB
import com.test.networkstat.managers.PrefManager
import com.test.networkstat.services.AppService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

    fun getFileSize(value: Long): String {
        var size = value
        if (value == 0L) return "0"
        if (value < 0) size *= -1
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        var res = DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
        if (value <= 0) res = ("-").plus(res)
        return res
    }

    fun getStartTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 12)
        date.set(Calendar.MINUTE, 54)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)
        return date.timeInMillis
    }

    fun getEndTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 12)
        date.set(Calendar.MINUTE, 46)
        date.set(Calendar.SECOND, 23)
        date.set(Calendar.MILLISECOND, 430)
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

    fun getLastCollectionEndTime(ctx: Context): Long {
        var time = PrefManager.createInstance(ctx).getLong(PrefManager.LAST_COLLECTION_END_TIME, 0L)
        if (time == 0L) {
            time = getEvenStartTime(System.currentTimeMillis())
        }
        return time
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

    fun findSharedUid(ctx: Context) {
        Log.d(TAG, "findSharedUid: ")
        val appInfoList = ctx.packageManager.getInstalledPackages(0)
        val uidMap = mutableMapOf<Int, ArrayList<String>>()
        for (appInfo in appInfoList) {
            if (uidMap.containsKey(appInfo.applicationInfo.uid)) {
                uidMap[appInfo.applicationInfo.uid]!!.add(appInfo.packageName)
            } else {
                uidMap[appInfo.applicationInfo.uid] = arrayListOf(appInfo.packageName)
            }
        }
        for (uid in uidMap) {
            if (uid.value.size > 1) {
                Log.e(TAG, "uid: ${uid.key} : No of apps Share: " + uid.value.size)
                for (app in uid.value) {
                    Log.d(TAG, "pkgName: ${app}")
                }
            }
        }
    }

    fun reset() {
        GlobalScope.launch {
            RoomDB.getDatabase().appDataUsageDao().deleteAll()
            RoomDB.getDatabase().deviceDataUsageDao().deleteAll()
            PrefManager.createInstance(App.getInstance()).clearPref()
        }
    }

    fun getEvenStartTime(ct: Long): Long {
        val period = TimeUnit.HOURS.toMillis(2)
        val prevWholeTime = (ct / period) * period
        Log.d("akshay", "getEvenStartTime $prevWholeTime")
        return prevWholeTime
    }
}