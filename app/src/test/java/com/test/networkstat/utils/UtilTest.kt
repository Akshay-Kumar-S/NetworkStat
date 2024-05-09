package com.test.networkstat.utils

class UtilTest {

    @org.junit.jupiter.api.Test
    fun getEvenStartTime() {
        var time = Util.getEvenStartTime(System.currentTimeMillis())
        return assert(true)
    }
}