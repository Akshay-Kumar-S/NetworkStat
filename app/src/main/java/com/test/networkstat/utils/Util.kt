package com.test.networkstat.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import com.test.networkstat.App
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Util {

    fun getUid(appName: String): Int {
        var uid = 0
        try {
            uid = App.getInstance().packageManager.getApplicationInfo(appName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return uid
    }

    fun getDate(milliSeconds: Long, format: String): String? {
        // Create a DateFormatter object for displaying date in specified format.
        //"dd/MM/yyyy hh:mm:ss.SSS"
        val formatter = SimpleDateFormat(format)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }

    fun getStartTime(): Long {
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, 15)
        date.set(Calendar.MINUTE, 30)
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
}