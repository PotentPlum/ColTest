package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSetTest {
    @Test
    fun `numbers sort the way they're printed`() {
        val numbers = listOf("10", "TG05", "2", "132", "H03", "1", "TG10", "SV54", "?")
        assertEquals(
            listOf("1", "2", "10", "132", "?", "H03", "SV54", "TG05", "TG10"),
            numbers.sortedWith(CardNumberOrder),
        )
    }

    @Test
    fun `numbers with a letter after them stay next to their number`() {
        assertEquals(listOf("4", "4a", "5"), listOf("5", "4a", "4").sortedWith(CardNumberOrder))
    }

    @Test
    fun `one tap adds the only version`() {
        assertEquals(CardVariant.NORMAL, CardVariant.quickAddFor(emptyList()))
        assertEquals(CardVariant.HOLO, CardVariant.quickAddFor(listOf(CardVariant.HOLO)))
    }

    @Test
    fun `one tap adds the regular print of a normal and holo card`() {
        assertEquals(CardVariant.NORMAL, CardVariant.quickAddFor(listOf(CardVariant.NORMAL, CardVariant.REVERSE_HOLO)))
        assertEquals(CardVariant.HOLO, CardVariant.quickAddFor(listOf(CardVariant.HOLO, CardVariant.REVERSE_HOLO)))
        assertEquals(CardVariant.NORMAL, CardVariant.quickAddFor(listOf(CardVariant.NORMAL, CardVariant.HOLO)))
    }

    @Test
    fun `more versions than a normal and a holo need a choice`() {
        assertNull(CardVariant.quickAddFor(listOf(CardVariant.NORMAL, CardVariant.HOLO, CardVariant.REVERSE_HOLO)))
        assertNull(CardVariant.quickAddFor(listOf(CardVariant.FIRST_EDITION, CardVariant.UNLIMITED)))
        assertNull(CardVariant.quickAddFor(listOf(CardVariant.FIRST_EDITION_HOLO, CardVariant.UNLIMITED_HOLO)))
    }

    @Test
    fun `sets match on name, code and series`() {
        val set = CardSet(
            id = "me55",
            name = "30th Celebration",
            series = "Mega Evolution",
            releaseDate = "2026/09/16",
            printedTotal = 128,
            total = 161,
            code = "30C",
            logoUrl = "",
            symbolUrl = "",
        )
        assertTrue(set.matches("30c"))
        assertTrue(set.matches("celebration"))
        assertTrue(set.matches("mega"))
        assertTrue(set.matches("2026"))
        assertFalse(set.matches("base"))
    }

    @Test
    fun `only cards with a dex number count toward the pokedex`() {
        val card = TcgCard("me55-129", "Ultra Ball", "129", null, "me55", "30th Celebration", "Mega Evolution", "2026/09/16", "s", "l")
        assertFalse(SetCard(card, 0).countsTowardDex)
        assertTrue(SetCard(card.copy(name = "Articuno"), 144).countsTowardDex)
    }
}
