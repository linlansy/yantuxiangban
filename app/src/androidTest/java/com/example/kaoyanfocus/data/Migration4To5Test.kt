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
class Migration4To5Test {
    private val dbName = "migration-4-5-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun oldEquipmentAndRewardsMoveToTheNewSlots() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO pet_profiles (id,unlockedAt,name,namedAt,equippedExpression,equippedOutfit,equippedAction,placedFurnitureCsv) VALUES (1,1,'星星',2,'expression_happy','outfit_glasses','action_spin','')")
            execSQL("INSERT INTO pet_unlocks (itemKey,itemType,sourceLevel,unlockedAt,sourceType) VALUES ('action_spin','ACTION',13,3,'LEVEL')")
            execSQL("INSERT INTO pet_unlocks (itemKey,itemType,sourceLevel,unlockedAt,sourceType) VALUES ('outfit_glasses','OUTFIT',9,3,'LEVEL')")
            execSQL("INSERT INTO reward_codes (id,codeHash,displayCode,points,active,createdAt,usedAt,rewardType,rewardPayload) VALUES (1,'hash','STAR',0,1,1,NULL,'PET_ITEM','outfit_moonlight')")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_4_5)
        migrated.query("SELECT equippedOutfit,equippedEyeAccessory,equippedNeckAccessory,highestRewardLevel,showOnFocus FROM pet_profiles WHERE id=1").use {
            it.moveToFirst()
            assertEquals("", it.getString(0))
            assertEquals("outfit_glasses", it.getString(1))
            assertEquals("", it.getString(2))
            assertEquals(13, it.getInt(3))
            assertEquals(1, it.getInt(4))
        }
        migrated.query("SELECT itemKey FROM pet_unlocks WHERE sourceLevel=13").use {
            it.moveToFirst(); assertEquals("action_stretch", it.getString(0))
        }
        migrated.query("SELECT rewardPayload FROM reward_codes WHERE id=1").use {
            it.moveToFirst(); assertEquals("outfit_starry", it.getString(0))
        }
        migrated.close()
    }
}
