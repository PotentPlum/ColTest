package com.potentplum.dextracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DexSearchTest {
    @Test
    fun `blank query matches everything`() {
        assertTrue(DexSearch.matches("", 25, "Pikachu"))
        assertTrue(DexSearch.matches("   ", 25, "Pikachu"))
    }

    @Test
    fun `numbers jump to that exact entry`() {
        assertTrue(DexSearch.matches("25", 25, "Pikachu"))
        assertTrue(DexSearch.matches("#025", 25, "Pikachu"))
        assertTrue(DexSearch.matches("# 25", 25, "Pikachu"))
        assertFalse(DexSearch.matches("2", 25, "Pikachu"))
        assertFalse(DexSearch.matches("250", 25, "Pikachu"))
    }

    @Test
    fun `names match anywhere, ignoring case`() {
        assertTrue(DexSearch.matches("pika", 25, "Pikachu"))
        assertTrue(DexSearch.matches("CHU", 25, "Pikachu"))
        assertFalse(DexSearch.matches("raichu", 25, "Pikachu"))
    }

    @Test
    fun `names ignore punctuation and accents`() {
        assertTrue(DexSearch.matches("mr mime", 122, "Mr. Mime"))
        assertTrue(DexSearch.matches("farfetchd", 83, "Farfetch'd"))
        assertTrue(DexSearch.matches("flabebe", 669, "Flabébé"))
        assertTrue(DexSearch.matches("type null", 772, "Type: Null"))
        assertTrue(DexSearch.matches("hooh", 250, "Ho-Oh"))
    }

    @Test
    fun `punctuation alone matches nothing`() {
        assertFalse(DexSearch.matches(".", 122, "Mr. Mime"))
    }
}
