package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFormatterTest {
    @Test
    fun `capitalises plain slugs`() {
        assertEquals("Pikachu", NameFormatter.fromSlug("pikachu"))
    }

    @Test
    fun `handles special gen 1 to 3 names`() {
        assertEquals("Nidoran♀", NameFormatter.fromSlug("nidoran-f"))
        assertEquals("Mr. Mime", NameFormatter.fromSlug("mr-mime"))
        assertEquals("Farfetch'd", NameFormatter.fromSlug("farfetchd"))
        assertEquals("Ho-Oh", NameFormatter.fromSlug("ho-oh"))
    }

    @Test
    fun `generation ranges cover the national dex without gaps`() {
        assertEquals(Generation.GEN_1, Generation.forDex(151))
        assertEquals(Generation.GEN_2, Generation.forDex(152))
        assertEquals(Generation.GEN_3, Generation.forDex(386))
        assertEquals("#025", 25.dexLabel())
    }
}
