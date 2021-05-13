package com.test.networkstat.database.models

data class AppUsage(
    val uid: Int,
    var txBytes: Long = 0,
    var rxBytes: Long = 0
)
