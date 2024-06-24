package com.test.networkstat

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.test.networkstat.database.models.QueryConfig
import com.test.networkstat.database.models.TimePeriod
import com.test.networkstat.utils.TestUtil
import com.test.networkstat.utils.Util
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private var TAG = "akshay"
    private var time: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        setContentView(R.layout.activity_main)
        askUsageAccessPermission()
//        get30minWindows()
        Util.getEvenStartTime(System.currentTimeMillis())
    }

    fun getUsageFromStartUp(view: View) {
        val testUtil = TestUtil()
        for (i in 1..1) {
            val queryConfig = QueryConfig(i)
            queryConfig.timePeriod = TimePeriod(0, System.currentTimeMillis())
            testUtil.getAppDataUsageFromStartup(this.applicationContext, queryConfig)
            testUtil.getDeviceDataUsageFromStartup(this.applicationContext, queryConfig)
            testUtil.printAppDeviceUsageDifferent()
        }
    }

    fun get2hrWindow(view: View) {
        Log.e(TAG, "get2hrWindow: ")
        time = System.currentTimeMillis()
        val endTime = Util.getEvenStartTime(time)
        val startTime = endTime - TimeUnit.HOURS.toMillis(2)
        val testUtil = TestUtil()
        for (i in 1..1) {
            val queryConfig = QueryConfig(i)
            queryConfig.timePeriod = TimePeriod(startTime, endTime)
            //testUtil.getAppDataUsageFromStartup(this.applicationContext, queryConfig)
            testUtil.getDeviceDataUsageFromStartup(this.applicationContext, queryConfig)
            testUtil.printAppDeviceUsageDifferent()
        }
    }

    private fun get30minWindows() {
        Log.e(TAG, "get30minWindows: ")
        var endTime = Util.getEvenStartTime(time)
        var startTime = endTime - TimeUnit.HOURS.toMillis(2)
        val testUtil = TestUtil()
        for (i in 0..1) {
            val queryConfig = QueryConfig(i)
            queryConfig.timePeriod = TimePeriod(startTime, endTime)
            for (i in 0..3) {
                endTime = startTime + TimeUnit.MINUTES.toMillis(30)
                testUtil.getAppDataUsageFromStartup(this.applicationContext, queryConfig)
                testUtil.getDeviceDataUsageFromStartup(this.applicationContext, queryConfig)
                testUtil.printAppDeviceUsageDifferent()
                startTime = endTime
            }
        }
    }

    private fun askUsageAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!getUsageAccessPermission()) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun getUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startService(view: View) {
        Util.startService(this)
    }

    fun stopService(view: View) {
        Util.stopService(this)
        Util.reset()
    }

    fun printAppUid(view: View) {
        Util.findSharedUid(this)
    }
}