package com.example.kaoyanfocus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    @Test fun catalogHasStableUniqueKeysAndManualFallback() {
        assertTrue(FoodCatalog.items.size >= 100)
        assertEquals(FoodCatalog.items.size, FoodCatalog.items.map { it.key }.distinct().size)
        assertEquals("其他食物", FoodCatalog.find("unknown-key").name)
    }

    @Test fun onlyChocolateIsRefused() {
        assertEquals(listOf("chocolate"), FoodCatalog.items.filter { it.unsafe }.map { it.key })
        listOf("coffee", "tea", "alcohol", "grape", "hotpot", "malatang", "rice", "apple", "chicken", "yogurt").forEach {
            assertFalse("$it should be feedable", FoodCatalog.find(it).unsafe)
        }
    }

    @Test fun newlyAddedChineseDishesHaveDedicatedCategories() {
        assertEquals("guobaorou", FoodCatalog.find("锅包肉").key)
        assertEquals("grilled_fish", FoodCatalog.find("烤鱼").key)
        assertEquals("claypot_rice", FoodCatalog.find("煲仔饭").key)
        assertFalse(FoodCatalog.find("锅包肉").unsafe)
        assertFalse(FoodCatalog.find("烤鱼").unsafe)
        assertFalse(FoodCatalog.find("煲仔饭").unsafe)
    }

    @Test fun recognizerUsesCompleteMealsInsteadOfBareStaples() {
        listOf("蛋炒饭", "热干面", "牛肉面", "辣肉面", "拌面", "烩面", "螺蛳粉", "黄焖鸡米饭").forEach {
            assertTrue("$it should be selectable", FoodCatalog.items.any { food -> food.name == it })
        }
        listOf("米饭", "面条", "鸡肉", "牛肉", "豆腐").forEach {
            assertFalse("$it should only remain for old diary records", FoodCatalog.items.any { food -> food.name == it })
            assertEquals(it, FoodCatalog.find(it).name)
        }
    }
}
