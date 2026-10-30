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
    fun `drops alolan and galarian forms`() {
        assertFalse(CardRules.isAllowed(card("Alolan Vulpix", "Pokémon", "Basic")))
        assertFalse(CardRules.isAllowed(card("Galarian Ponyta", "Pokémon", "Basic")))
        assertFalse(CardRules.isAllowed(card("Galarian Moltres V", "Pokémon", "Basic", "V")))
    }
}
