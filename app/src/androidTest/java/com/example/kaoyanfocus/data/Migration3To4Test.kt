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
class Migration3To4Test {
    private val dbName = "migration-3-4-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun oldStudyAndRestDataSurvive() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO study_sessions (id,startedAt,endedAt,minutes,points,categoryId,categoryName,note,durationSeconds,pauseCount,nightSeconds,earlySeconds,startDate) VALUES (1,1,60001,1,0,NULL,'学习','',60,0,0,0,'2026-08-01')")
            execSQL("INSERT INTO rest_days (`date`,redemptionId,usedAt) VALUES ('2026-08-02',9,100)")
            close()
        }
        val migrated = helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4)
        migrated.query("SELECT themeKeyAtStart,lateNightSeconds FROM study_sessions WHERE id=1").use {
            it.moveToFirst(); assertEquals("", it.getString(0)); assertEquals(0L, it.getLong(1))
        }
        migrated.query("SELECT redemptionId,sourceType FROM rest_days WHERE `date`='2026-08-02'").use {
            it.moveToFirst(); assertEquals(9L, it.getLong(0)); assertEquals("LEGACY_CARD", it.getString(1))
        }
        migrated.close()
    }
}
