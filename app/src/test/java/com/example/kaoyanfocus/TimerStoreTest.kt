package com.example.kaoyanfocus

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimerStoreTest {
    private val zone = ZoneId.systemDefault()

    @Test fun `late night interval is assigned to its evening bucket`() {
        val date = LocalDate.of(2026, 8, 27)
        val start = date.atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atTime(0, 30).atZone(zone).toInstant().toEpochMilli()

        val result = TimerStore.analyzeInterval(start, end)

        assertEquals(1800, result.nightSeconds)
        assertEquals(0, result.earlySeconds)
        assertEquals(3600L, result.lateNightBuckets["2026-08-27"] ?: -1L)
    }

    @Test fun `early bird interval counts five to nine`() {
        val date = LocalDate.of(2026, 8, 27)
        val start = date.atTime(5, 0).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()

        assertEquals(3600, TimerStore.analyzeInterval(start, end).earlySeconds)
    }
}
