package com.example.kaoyanfocus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpriteBackgroundTest {
    @Test fun recognizesGeneratedCheckerboardColors() {
        assertTrue(isGeneratedCheckerboardPixel(0xFFFEFEFE.toInt()))
        assertTrue(isGeneratedCheckerboardPixel(0xFFF2F2F2.toInt()))
        assertTrue(isGeneratedCheckerboardPixel(0xFFFDFBF9.toInt()))
    }

    @Test fun preservesCharacterAndOutfitColors() {
        assertFalse(isGeneratedCheckerboardPixel(0xFF0F2F78.toInt()))
        assertFalse(isGeneratedCheckerboardPixel(0xFFC18A45.toInt()))
        assertFalse(isGeneratedCheckerboardPixel(0xFFFFD45A.toInt()))
        assertFalse(isGeneratedCheckerboardPixel(0xFFB9DFFF.toInt()))
    }
}
