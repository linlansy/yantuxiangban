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
class Migration6To7Test {
    private val dbName = "migration-6-7-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun addsFoodTablesWithoutChangingExistingPetData() {
        helper.createDatabase(dbName, 6).apply {
            execSQL("INSERT INTO pet_profiles (id, unlockedAt, name, namedAt, equippedExpression, equippedOutfit, equippedAction, placedFurnitureCsv, equippedEyeAccessory, equippedNeckAccessory, equippedHeadAccessory, highestRewardLevel, showOnFocus) VALUES (1, 1, '星星', 1, '', 'outfit_school', '', '', '', '', '', 5, 1)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 7, true, AppDatabase.MIGRATION_6_7).use { db ->
            db.query("SELECT name, equippedOutfit FROM pet_profiles WHERE id = 1").use {
                it.moveToFirst(); assertEquals("星星", it.getString(0)); assertEquals("outfit_school", it.getString(1))
            }
            db.execSQL("INSERT INTO food_records (photoPath, sourceType, recognizedKey, recognizedName, candidatesCsv, confidence, safetyState, status, capturedAt, confirmedAt, fedAt, feedDate, aiProvider, aiModel) VALUES (NULL, 'GALLERY', 'apple', '苹果', '', 0.9, 'SAFE', 'FED', 1, 1, 2, '2026-09-01', 'QWEN', 'qwen3-vl-flash')")
            db.query("SELECT COUNT(*) FROM food_records WHERE status = 'FED'").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        }
    }
}
