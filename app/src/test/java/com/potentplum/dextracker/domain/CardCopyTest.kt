package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardCopyTest {
    @Test
    fun `parses api variant keys in display order and drops unknown ones`() {
        assertEquals(
            listOf(CardVariant.HOLO, CardVariant.REVERSE_HOLO),
            CardVariant.parse(listOf("reverseHolofoil", "holofoil", "somethingNew")),
        )
        assertEquals(
            listOf(CardVariant.UNLIMITED, CardVariant.FIRST_EDITION),
            CardVariant.parse(listOf("1stEditionNormal", "unlimited")),
        )
    }

    @Test
    fun `cards without variant data can be logged as a normal print`() {
        assertEquals(listOf(CardVariant.NORMAL), CardVariant.optionsFor(emptyList()))
    }

    @Test
    fun `default variant is the regular print`() {
        assertEquals(CardVariant.HOLO, CardVariant.defaultFor(listOf(CardVariant.HOLO, CardVariant.REVERSE_HOLO)))
        assertEquals(CardVariant.NORMAL, CardVariant.defaultFor(listOf(CardVariant.REVERSE_HOLO, CardVariant.NORMAL)))
        assertEquals(
            CardVariant.UNLIMITED_HOLO,
            CardVariant.defaultFor(listOf(CardVariant.UNLIMITED_HOLO, CardVariant.FIRST_EDITION_HOLO)),
        )
        assertEquals(CardVariant.NORMAL, CardVariant.defaultFor(emptyList()))
    }

    @Test
    fun `unknown condition and language codes fall back to the defaults`() {
        assertEquals(CardCondition.NEAR_MINT, CardCondition.fromCode("??"))
        assertEquals(CardCondition.LIGHT_PLAYED, CardCondition.fromCode("LP"))
        assertEquals(CardLanguage.ENGLISH, CardLanguage.fromCode(""))
        assertEquals(CardLanguage.DUTCH, CardLanguage.fromCode("NL"))
    }

    @Test
    fun `reverse holos use the reverse holo price when there is one`() {
        val prices = CardPrices(trend = 6.8, reverseHoloTrend = 20.37)
        assertEquals(20.37, prices.unitValue(CardVariant.REVERSE_HOLO)!!, 0.0)
        assertEquals(6.8, prices.unitValue(CardVariant.HOLO)!!, 0.0)
        assertEquals(6.8, CardPrices(6.8, 0.0).unitValue(CardVariant.REVERSE_HOLO)!!, 0.0)
    }

    @Test
    fun `zero or missing prices count as unknown`() {
        assertNull(CardPrices(0.0, null).unitValue(CardVariant.NORMAL))
        assertNull(CardPrices.NONE.unitValue(CardVariant.REVERSE_HOLO))
    }
}
