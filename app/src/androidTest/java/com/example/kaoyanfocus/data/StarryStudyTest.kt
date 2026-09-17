package com.example.kaoyanfocus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kaoyanfocus.ThemeCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StarryStudyTest {
    @Test fun simulatedStudyStoresStartThemeAndAwardsAStar() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries().build()
            try {
                val repo = AppRepository(context, db)
                repo.seed()
                db.dao().insertRedemption(
                    Redemption(
                        productId = 0,
                        productName = "星空夜读主题",
                        cost = 30,
                        redeemedAt = System.currentTimeMillis(),
                        entitlement = "测试",
                        productType = "PERMANENT"
                    )
                )
                assertTrue(repo.setTheme(ThemeCatalog.STARRY))

                repo.addTestStudyDuration(Dates.today(), "12:00", 1, 0, "星空测试")

                val sessions = db.dao().sessionSnapshot()
                assertEquals(ThemeCatalog.STARRY, sessions.single().themeKeyAtStart)
                assertEquals(1, PetRules.starsFor(sessions))
                repo.setTheme(ThemeCatalog.DEFAULT)
            } finally {
                db.close()
            }
        }
    }

    @Test fun constellationRewardRequiresStarsAndCanOnlyBeClaimedOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val repo = AppRepository(context, db)
            repo.seed()
            db.dao().insertRedemption(Redemption(productId = 0, productName = "星空夜读主题", cost = 30,
                redeemedAt = System.currentTimeMillis(), entitlement = "测试", productType = "PERMANENT"))
            assertTrue(repo.setTheme(ThemeCatalog.STARRY))
            repo.addTestStudyDuration(Dates.today(), "09:00", 7, 0, "星图奖励测试")

            assertTrue(repo.claimConstellationReward(7, "北斗七星", 1))
            assertFalse(repo.claimConstellationReward(7, "北斗七星", 1))
            assertFalse(repo.claimConstellationReward(30, "双鱼座", 5))
            assertEquals(1, db.dao().ledgerSnapshot().count { it.businessKey == "constellation_reward:7" })
        } finally {
            db.close()
        }
    }
}
