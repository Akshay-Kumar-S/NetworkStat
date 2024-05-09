package com.test.networkstat.managers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.test.networkstat.BuildConfig

class PrefManager {
    private val name = BuildConfig.APPLICATION_ID
    private val mode = Context.MODE_PRIVATE
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    companion object {
        const val LAST_COLLECTION_END_TIME = "last_usage_end_time"
        const val AGGREGATION_TIME = "aggregation_time"

        fun createInstance(context: Context): PrefManager {
            val prefManager = PrefManager()
            prefManager.initPref(context)
            return prefManager
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun initPref(context: Context) {
        if (!this::sharedPreferences.isInitialized)
            sharedPreferences = context.getSharedPreferences(name, mode)
        editor = sharedPreferences.edit()
    }

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).commit()
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defValue)
    }

    fun putString(key: String, value: String) {
        editor.putString(key, value).commit()
    }

    fun getString(key: String, defValue: String?): String? {
        return sharedPreferences.getString(key, defValue)
    }

    fun putLong(key: String, value: Long) {
        editor.putLong(key, value).commit()
    }

    fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    fun remove(key: String): Boolean {
        return editor.remove(key).commit()
    }

    fun clearPref(): Boolean {
        return editor.clear().commit()
    }
}