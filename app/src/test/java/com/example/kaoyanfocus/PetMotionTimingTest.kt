package com.example.kaoyanfocus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMotionTimingTest {
    @Test
    fun readingIsCalmAndHasAVisibleRest() {
        assertTrue(PetMotionTiming.READ >= PetMotionTiming.WAVE * 2)
        assertTrue(PetMotionTiming.READING_REST >= 1_000L)
        assertEquals(PetMotionTiming.READ, PetMotionTiming.forAction("action_read"))
    }

    @Test
    fun everyInteractiveMotionUsesTheSlowerPacing() {
        val actions = listOf("action_wave", "action_stretch", "action_celebrate", "action_drink", "action_lie", "action_pet")
        assertTrue(actions.all { PetMotionTiming.forAction(it) >= 2_800L })
    }
}
