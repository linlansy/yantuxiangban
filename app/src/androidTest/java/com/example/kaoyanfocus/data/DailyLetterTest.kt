package com.example.kaoyanfocus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLetterTest {
    @Test fun oneLetterIsIssuedWhenDailyStudyReachesEightHours() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries().build()
            try {
                val repo = AppRepository(context, db)
                repo.seed()
                db.dao().insertPetProfile(PetProfile(unlockedAt = 1, name = "小星", namedAt = 1))
                val date = Dates.today()

                repo.addTestStudyDuration(date, "09:00", 7, 59, "来信测试")
                assertEquals(0, db.dao().petLetterSnapshot().size)

                repo.addTestStudyDuration(date, "17:00", 0, 1, "来信测试")
                assertEquals(1, db.dao().petLetterSnapshot().size)
                assertEquals("day:$date", db.dao().petLetterSnapshot().single().nightKey)

                repo.addTestStudyDuration(date, "18:00", 1, 0, "来信测试")
                assertEquals(1, db.dao().petLetterSnapshot().size)
            } finally {
                db.close()
            }
        }
    }
}
