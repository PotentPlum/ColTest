package com.potentplum.dextracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardRulesTest {
    private fun card(name: String, supertype: String = "Pokémon", vararg subtypes: String) =
        CardTraits(name, supertype, subtypes.toList())

    @Test
    fun `keeps regular pokemon cards`() {
        assertTrue(CardRules.isAllowed(card("Charizard", "Pokémon", "Stage 2")))
        assertTrue(CardRules.isAllowed(card("Charizard ex", "Pokémon", "Stage 2", "ex")))
        assertTrue(CardRules.isAllowed(card("Dark Charizard", "Pokémon", "Stage 2")))
    }

    @Test
    fun `supertype match ignores accents and case`() {
        assertTrue(CardRules.isAllowed(card("Pikachu", "Pokemon", "Basic")))
        assertTrue(CardRules.isAllowed(card("Pikachu", "POKÉMON", "Basic")))
    }

    @Test
    fun `drops trainer and energy supertypes`() {
        assertFalse(CardRules.isAllowed(card("Pikachu Doll", "Trainer", "Item")))
        assertFalse(CardRules.isAllowed(card("Lightning Energy", "Energy", "Basic")))
    }

    @Test
    fun `drops tag team cards`() {
        assertFalse(CardRules.isAllowed(card("Pikachu & Zekrom-GX", "Pokémon", "Basic", "TAG TEAM", "GX")))
    }

    @Test
    fun `flags every regional form alike`() {
        assertTrue(CardRules.isRegionalForm(card("Alolan Vulpix", "Pokémon", "Basic")))
        assertTrue(CardRules.isRegionalForm(card("Galarian Moltres V", "Pokémon", "Basic", "V")))
        assertTrue(CardRules.isRegionalForm(card("Hisuian Zoroark VSTAR", "Pokémon", "VSTAR")))
        assertTrue(CardRules.isRegionalForm(card("Paldean Wooper", "Pokémon", "Basic")))
    }

    @Test
    fun `regional forms are still valid pokemon cards`() {
        // Whether they count is the user's setting, applied after the rules engine.
        assertTrue(CardRules.isAllowed(card("Alolan Vulpix", "Pokémon", "Basic")))
    }

    @Test
    fun `base forms and regional evolutions are not regional forms`() {
        assertFalse(CardRules.isRegionalForm(card("Vulpix", "Pokémon", "Basic")))
        assertFalse(CardRules.isRegionalForm(card("Obstagoon", "Pokémon", "Stage 2")))
        assertFalse(CardRules.isRegionalForm(card("Sirfetch'd", "Pokémon", "Stage 1")))
    }
}
