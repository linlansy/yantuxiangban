package com.example.kaoyanfocus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodFeedingTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = AppRepository(context, db)
    }

    @After fun close() { db.close() }

    @Test fun foodCanOnlyBeFedOnceAndDailyLimitIsThree() = runBlocking {
        val ids = (1..4).map { db.dao().insertFoodRecord(FoodRecord(recognizedKey = "apple", recognizedName = "苹果")) }
        ids.take(3).forEach { assertTrue(repo.feedPet(it).contains("亲密度 +1")) }
        assertTrue(repo.feedPet(ids.first()).contains("已经吃过"))
        assertTrue(repo.feedPet(ids.last()).contains("明天再喂"))
        assertEquals(3, db.dao().affection().first())
    }

    @Test fun unsafeFoodIsRefusedWithoutUsingDailyAllowance() = runBlocking {
        val unsafe = db.dao().insertFoodRecord(FoodRecord(recognizedKey = "chocolate", recognizedName = "巧克力", safetyState = "UNSAFE", status = "REFUSED"))
        assertTrue(repo.feedPet(unsafe).contains("不适合"))
        assertEquals(0, db.dao().fedCount(Dates.today()))
        assertEquals(0, db.dao().affection().first())
    }
}
