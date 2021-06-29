package com.test.networkstat.database.models

import java.util.concurrent.TimeUnit

data class QueryConfig(val networkType: Int) {
    var collectionInterval = TimeUnit.MINUTES.toMillis(15)
    lateinit var timePeriod: TimePeriod
    var prevCollectionEndTime: Long = 0
}
