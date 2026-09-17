package com.example.kaoyanfocus

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

data class TimerSnapshot(
    val startedAt: Long = 0,
    val segmentStartedAt: Long = 0,
    val accumulatedSeconds: Long = 0,
    val running: Boolean = false,
    val categoryId: Long = 0,
    val categoryName: String = "学习",
    val note: String = "",
    val pauseCount: Int = 0,
    val nightSeconds: Long = 0,
    val earlySeconds: Long = 0,
    val warned12Hours: Boolean = false,
    val themeKeyAtStart: String = "",
    val pauseEggMask: Int = 0,
    val lateNightBuckets: String = ""
) {
    fun elapsed(now: Long = System.currentTimeMillis()): Long =
        accumulatedSeconds + if (running && segmentStartedAt > 0) max(0, (now - segmentStartedAt) / 1000) else 0
}

data class IntervalStats(
    val nightSeconds: Long,
    val earlySeconds: Long,
    val lateNightBuckets: Map<String, Long>
)

object TimerStore {
    private const val FILE = "active_study_timer"
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context): TimerSnapshot? {
        val p = prefs(context)
        val started = p.getLong("startedAt", 0)
        if (started == 0L) return null
        return TimerSnapshot(
            startedAt = started,
            segmentStartedAt = p.getLong("segmentStartedAt", 0),
            accumulatedSeconds = p.getLong("accumulatedSeconds", 0),
            running = p.getBoolean("running", false),
            categoryId = p.getLong("categoryId", 0),
            categoryName = p.getString("categoryName", "学习") ?: "学习",
            note = p.getString("note", "") ?: "",
            pauseCount = p.getInt("pauseCount", 0),
            nightSeconds = p.getLong("nightSeconds", 0),
            earlySeconds = p.getLong("earlySeconds", 0),
            warned12Hours = p.getBoolean("warned12Hours", false),
            themeKeyAtStart = p.getString("themeKeyAtStart", "") ?: "",
            pauseEggMask = p.getInt("pauseEggMask", 0),
            lateNightBuckets = p.getString("lateNightBuckets", "") ?: ""
        )
    }

    fun save(context: Context, value: TimerSnapshot) {
        prefs(context).edit()
            .putLong("startedAt", value.startedAt)
            .putLong("segmentStartedAt", value.segmentStartedAt)
            .putLong("accumulatedSeconds", value.accumulatedSeconds)
            .putBoolean("running", value.running)
            .putLong("categoryId", value.categoryId)
            .putString("categoryName", value.categoryName)
            .putString("note", value.note)
            .putInt("pauseCount", value.pauseCount)
            .putLong("nightSeconds", value.nightSeconds)
            .putLong("earlySeconds", value.earlySeconds)
            .putBoolean("warned12Hours", value.warned12Hours)
            .putString("themeKeyAtStart", value.themeKeyAtStart)
            .putInt("pauseEggMask", value.pauseEggMask)
            .putString("lateNightBuckets", value.lateNightBuckets)
            .apply()
    }

    fun start(context: Context, categoryId: Long, categoryName: String, note: String, themeKey: String): TimerSnapshot {
        val now = System.currentTimeMillis()
        return TimerSnapshot(now, now, running = true, categoryId = categoryId, categoryName = categoryName, note = note, themeKeyAtStart = themeKey)
            .also { save(context, it) }
    }

    fun pause(context: Context): TimerSnapshot? {
        val old = load(context) ?: return null
        if (!old.running) return old
        val now = System.currentTimeMillis()
        val segmentSeconds = max(0, (now - old.segmentStartedAt) / 1000)
        val value = old.copy(
            segmentStartedAt = 0,
            accumulatedSeconds = old.accumulatedSeconds + segmentSeconds,
            running = false,
            pauseCount = old.pauseCount + 1,
            nightSeconds = old.nightSeconds + secondsInWindow(old.segmentStartedAt, now, 0, 4),
            earlySeconds = old.earlySeconds + secondsInWindow(old.segmentStartedAt, now, 5, 9),
            lateNightBuckets = mergeNightBuckets(old.lateNightBuckets, lateNightBuckets(old.segmentStartedAt, now))
        )
        save(context, value)
        return value
    }

    fun resume(context: Context): TimerSnapshot? {
        val old = load(context) ?: return null
        if (old.running) return old
        return old.copy(segmentStartedAt = System.currentTimeMillis(), running = true).also { save(context, it) }
    }

    fun updateContent(context: Context, categoryId: Long, categoryName: String, note: String) {
        load(context)?.let { save(context, it.copy(categoryId = categoryId, categoryName = categoryName, note = note)) }
    }

    fun finalize(context: Context, now: Long = System.currentTimeMillis()): TimerSnapshot? {
        val old = load(context) ?: return null
        if (!old.running) return old
        val segmentSeconds = max(0, (now - old.segmentStartedAt) / 1000)
        return old.copy(
            segmentStartedAt = 0,
            accumulatedSeconds = old.accumulatedSeconds + segmentSeconds,
            running = false,
            nightSeconds = old.nightSeconds + secondsInWindow(old.segmentStartedAt, now, 0, 4),
            earlySeconds = old.earlySeconds + secondsInWindow(old.segmentStartedAt, now, 5, 9),
            lateNightBuckets = mergeNightBuckets(old.lateNightBuckets, lateNightBuckets(old.segmentStartedAt, now))
        ).also { save(context, it) }
    }

    fun markPauseEggShown(context: Context, bit: Int): TimerSnapshot? {
        val old = load(context) ?: return null
        return old.copy(pauseEggMask = old.pauseEggMask or bit).also { save(context, it) }
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()

    private fun secondsInWindow(startMillis: Long, endMillis: Long, fromHour: Int, toHour: Int): Long {
        if (startMillis <= 0 || endMillis <= startMillis) return 0
        val zone = ZoneId.systemDefault()
        var date = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        var seconds = 0L
        while (!date.isAfter(last)) {
            val from: ZonedDateTime = date.atTime(fromHour, 0).atZone(zone)
            val to: ZonedDateTime = date.atTime(toHour, 0).atZone(zone)
            val overlapStart = max(startMillis, from.toInstant().toEpochMilli())
            val overlapEnd = minOf(endMillis, to.toInstant().toEpochMilli())
            if (overlapEnd > overlapStart) seconds += (overlapEnd - overlapStart) / 1000
            date = date.plusDays(1)
        }
        return seconds
    }

    fun decodeNightBuckets(value: String): Map<String, Long> = value.split(',').mapNotNull { item ->
        val parts = item.split('=')
        if (parts.size == 2) parts[1].toLongOrNull()?.let { parts[0] to it } else null
    }.toMap()

    fun encodeNightBuckets(value: Map<String, Long>): String =
        value.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" }

    fun analyzeInterval(startMillis: Long, endMillis: Long): IntervalStats = IntervalStats(
        nightSeconds = secondsInWindow(startMillis, endMillis, 0, 4),
        earlySeconds = secondsInWindow(startMillis, endMillis, 5, 9),
        lateNightBuckets = lateNightBuckets(startMillis, endMillis)
    )

    private fun mergeNightBuckets(encoded: String, addition: Map<String, Long>): String {
        val result = decodeNightBuckets(encoded).toMutableMap()
        addition.forEach { (key, seconds) -> result[key] = (result[key] ?: 0) + seconds }
        return encodeNightBuckets(result)
    }

    private fun lateNightBuckets(startMillis: Long, endMillis: Long): Map<String, Long> {
        if (startMillis <= 0 || endMillis <= startMillis) return emptyMap()
        val zone = ZoneId.systemDefault()
        var evening = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate().minusDays(1)
        val last = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val result = linkedMapOf<String, Long>()
        while (!evening.isAfter(last)) {
            val from = evening.atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
            val to = evening.plusDays(1).atTime(4, 0).atZone(zone).toInstant().toEpochMilli()
            val overlapStart = max(startMillis, from)
            val overlapEnd = minOf(endMillis, to)
            if (overlapEnd > overlapStart) result[evening.toString()] = (overlapEnd - overlapStart) / 1000
            evening = evening.plusDays(1)
        }
        return result
    }
}
