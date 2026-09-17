package com.example.kaoyanfocus.data

data class PointsBreakdown(val base: Int, val bonus: Int) { val total: Int get() = base + bonus }

object PointsCalculator {
    fun totalForDay(minutes: Int, rule: PointRuleVersion): PointsBreakdown {
        val eligible = minutes.coerceAtMost(rule.capMinutes).coerceAtLeast(0)
        val base = (eligible / rule.minutesPerPoint) * rule.pointsPerBlock
        val bonus = if (minutes >= rule.bonusMinutes) rule.bonusPoints else 0
        return PointsBreakdown(base, bonus)
    }
}
