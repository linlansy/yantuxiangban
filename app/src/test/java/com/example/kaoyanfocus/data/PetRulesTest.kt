package com.example.kaoyanfocus.data

import com.example.kaoyanfocus.ThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetRulesTest {
    @Test fun levelBoundariesAreStable() {
        assertEquals(1, PetRules.levelForXp(599))
        assertEquals(1, PetRules.levelForXp(899))
        assertEquals(2, PetRules.levelForXp(900))
        assertEquals(19, PetRules.levelForXp(29_999))
        assertEquals(20, PetRules.levelForXp(30_000))
        assertEquals(30_000, PetRules.cumulativeForLevel(20))
        assertEquals(500, PetRules.cumulativeForLevel(20) / 60)
        assertNull(PetRules.nextLevelXp(20))
    }

    @Test fun allNineteenRewardsHaveUniqueLevelsAndKeys() {
        assertEquals((2..20).toList(), PetRules.rewards.map { it.level })
        assertEquals(PetRules.rewards.size, PetRules.rewards.map { it.key }.distinct().size)
    }

    @Test fun confirmedMilestonesUseTotalStudyHours() {
        val hours = listOf(10, 15, 22, 30, 40, 52, 66, 82, 100, 120, 142, 166, 194, 226, 262, 302, 346, 394, 446, 500)
        assertEquals(hours.map { it * 60 }, (1..20).map(PetRules::cumulativeForLevel))
        assertEquals("action_stretch", PetRules.rewards.single { it.level == 13 }.key)
    }

    @Test fun starryMinutesAccumulateAcrossSessionsAndOtherThemesAreIgnored() {
        val sessions = listOf(
            StudySession(startedAt = 1, endedAt = 2, minutes = 30, points = 0, durationSeconds = 1800, themeKeyAtStart = ThemeCatalog.STARRY),
            StudySession(startedAt = 2, endedAt = 3, minutes = 30, points = 0, durationSeconds = 1800, themeKeyAtStart = ThemeCatalog.STARRY),
            StudySession(startedAt = 3, endedAt = 4, minutes = 120, points = 0, durationSeconds = 7200, themeKeyAtStart = ThemeCatalog.DEFAULT)
        )

        assertEquals(1, PetRules.starsFor(sessions))
    }
}
