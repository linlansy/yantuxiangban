package com.example.kaoyanfocus.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {
    private val dbName = "migration-5-6-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun replacesMisalignedAccessoriesWithoutLosingUnlocks() {
        helper.createDatabase(dbName, 5).apply {
            execSQL("INSERT INTO pet_profiles (id, unlockedAt, name, namedAt, equippedExpression, equippedOutfit, equippedAction, placedFurnitureCsv, equippedEyeAccessory, equippedNeckAccessory, equippedHeadAccessory, highestRewardLevel, showOnFocus) VALUES (1, 1, 'pet', 1, '', '', '', '', 'outfit_glasses', 'outfit_target_scarf', '', 20, 1)")
            execSQL("INSERT INTO pet_unlocks (itemKey, itemType, sourceLevel, unlockedAt, sourceType) VALUES ('outfit_glasses', 'ACCESSORY_EYE', 9, 1, 'LEVEL')")
            execSQL("INSERT INTO pet_unlocks (itemKey, itemType, sourceLevel, unlockedAt, sourceType) VALUES ('outfit_target_scarf', 'ACCESSORY_NECK', 19, 1, 'LEVEL')")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 6, true, AppDatabase.MIGRATION_5_6).use { db ->
            db.query("SELECT equippedEyeAccessory, equippedNeckAccessory FROM pet_profiles WHERE id = 1").use {
                it.moveToFirst()
                assertEquals("", it.getString(0))
                assertEquals("", it.getString(1))
            }
            db.query("SELECT itemKey, itemType FROM pet_unlocks ORDER BY sourceLevel").use {
                it.moveToFirst()
                assertEquals("furniture_cloud_cushion", it.getString(0))
                assertEquals("FURNITURE", it.getString(1))
                it.moveToNext()
                assertEquals("furniture_goal_trophy", it.getString(0))
                assertEquals("FURNITURE", it.getString(1))
            }
        }
    }
}
