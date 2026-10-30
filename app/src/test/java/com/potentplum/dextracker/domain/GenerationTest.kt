package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GenerationTest {
    @Test
    fun `ranges cover the whole national dex without gaps or overlaps`() {
        var expectedStart = 1
        Generation.entries.forEach { gen ->
            assertEquals("${gen.label} start", expectedStart, gen.dexRange.first)
            expectedStart = gen.dexRange.last + 1
        }
        assertEquals(1025, Generation.MAX_DEX)
        assertEquals(1025, Generation.entries.sumOf { it.size })
    }

    @Test
    fun `finds the generation for boundary numbers`() {
        assertEquals(Generation.GEN_1, Generation.forDex(151))
        assertEquals(Generation.GEN_2, Generation.forDex(152))
        assertEquals(Generation.GEN_4, Generation.forDex(387))
        assertEquals(Generation.GEN_8, Generation.forDex(905))
        assertEquals(Generation.GEN_9, Generation.forDex(1025))
    }

    @Test
    fun `labels pad to three digits`() {
        assertEquals("#025", 25.dexLabel())
        assertEquals("#1025", 1025.dexLabel())
    }
}
