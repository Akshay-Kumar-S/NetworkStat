package com.data.management

import android.app.usage.NetworkStats
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Util {
    fun getSimSubscriberId(ctx: Context): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                return null
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.subscriberId
        } catch (e: Exception) {
        }
        return ""
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

    fun getReadableState(state: Int): String {
        return when (state) {
            NetworkStats.Bucket.STATE_ALL -> "STATE_ALL"
            NetworkStats.Bucket.STATE_DEFAULT -> "STATE_DEFAULT"
            NetworkStats.Bucket.STATE_FOREGROUND -> "STATE_FOREGROUND"
            else -> "Unknown"
        }
    }

    fun getReadableRoaming(roaming: Int): String {
        return when (roaming) {
            NetworkStats.Bucket.ROAMING_ALL -> "ROAMING_ALL"
            NetworkStats.Bucket.ROAMING_NO -> "ROAMING_NO"
            NetworkStats.Bucket.ROAMING_YES -> "ROAMING_YES"
            else -> "Unknown"
        }
    }

    fun getReadableMetered(metered: Int): String {
        return when (metered) {
            NetworkStats.Bucket.METERED_ALL -> "METERED_ALL"
            NetworkStats.Bucket.METERED_NO -> "METERED_NO"
            NetworkStats.Bucket.METERED_YES -> "METERED_YES"
            else -> "Unknown"
        }
    }

    fun getReadableDefaultNetwork(network: Int): String {
        return when (network) {
            NetworkStats.Bucket.DEFAULT_NETWORK_ALL -> "DEFAULT_ALL"
            NetworkStats.Bucket.DEFAULT_NETWORK_NO -> "DEFAULT_NO"
            NetworkStats.Bucket.DEFAULT_NETWORK_YES -> "DEFAULT_YES"
            else -> "Unknown"
        }
    }

    fun getUid(ctx: Context, appName: String): Int {
        var uid = 0
        try {
            uid = ctx.packageManager.getApplicationInfo(appName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return uid
    }

    fun getFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }
}