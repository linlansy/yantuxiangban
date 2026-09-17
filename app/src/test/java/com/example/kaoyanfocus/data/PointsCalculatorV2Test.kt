package com.example.kaoyanfocus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PointsCalculatorV2Test {
    private val rule = PointRuleVersion(effectiveDate = "2000-01-01")
    @Test fun incompleteHourDoesNotScore() = assertEquals(0, PointsCalculator.totalForDay(59, rule).total)
    @Test fun completedHourScoresOne() = assertEquals(1, PointsCalculator.totalForDay(60, rule).total)
    @Test fun eightHoursIncludesBonus() = assertEquals(13, PointsCalculator.totalForDay(480, rule).total)
    @Test fun basePointsStopAtTwelveHours() = assertEquals(17, PointsCalculator.totalForDay(900, rule).total)
}
