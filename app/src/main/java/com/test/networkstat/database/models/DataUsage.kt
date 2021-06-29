package com.test.networkstat.database.models

data class DataUsage(
    var txBytes: Long = 0,
    var rxBytes: Long = 0,
    var txPackets: Long = 0,
    var rxPackets: Long = 0
)
