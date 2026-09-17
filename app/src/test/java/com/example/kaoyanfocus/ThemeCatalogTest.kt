package com.example.kaoyanfocus

import com.example.kaoyanfocus.data.AchievementUnlock
import com.example.kaoyanfocus.data.Redemption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {
    @Test
    fun unearnedThemesAreNotVisible() {
        assertEquals(listOf(ThemeCatalog.DEFAULT), ThemeCatalog.owned(emptyList(), emptyList()).map { it.key })
    }

    @Test
    fun cumulative100HoursUnlocksCinnamorollButDay8DoesNot() {
        val day8 = listOf(AchievementUnlock("day_8", 1L, 3))
        assertFalse(ThemeCatalog.owned(emptyList(), day8).any { it.key == ThemeCatalog.CINNAMOROLL })

        val total100 = listOf(AchievementUnlock("total_100", 2L, 10))
        assertTrue(ThemeCatalog.owned(emptyList(), total100).any { it.key == ThemeCatalog.CINNAMOROLL })
    }

    @Test
    fun buyingStarryThemeMakesOnlyStarryVisible() {
        val purchase = Redemption(
            productId = 9,
            productName = "星空夜读主题",
            cost = 30,
            redeemedAt = 1L,
            entitlement = "永久使用星空夜读主题",
            productType = "PERMANENT"
        )
        val keys = ThemeCatalog.owned(listOf(purchase), emptyList()).map { it.key }
        assertEquals(listOf(ThemeCatalog.DEFAULT, ThemeCatalog.STARRY), keys)
    }
}
