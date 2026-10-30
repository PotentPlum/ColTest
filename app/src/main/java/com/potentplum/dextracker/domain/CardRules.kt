package com.potentplum.dextracker.domain

import java.text.Normalizer

/** The fields of a TCG card the rules engine needs to make a decision. */
data class CardTraits(
    val name: String,
    val supertype: String,
    val subtypes: List<String>,
)

/**
 * Strict rules engine for TCG search results. Anything [isAllowed] rejects is dropped before
 * the user ever sees it, regardless of what the API returned. Regional forms are a user
 * setting, so they're flagged by [isRegionalForm] and filtered separately.
 */
object CardRules {
    /** Every regional variant prefix printed on cards, so the rule treats them all alike. */
    private val regionalTerms = listOf("alolan", "galarian", "hisuian", "paldean")

    /** Multi-Pokémon cards don't belong to a single Dex entry. */
    private val excludedSubtypes = listOf("tag team")

    fun isAllowed(card: CardTraits): Boolean {
        // Only Pokémon cards are collectible against a Dex entry; Trainer and Energy are dropped.
        if (card.supertype.normalized() != "pokemon") return false
        return card.subtypes.none { it.normalized() in excludedSubtypes }
    }

    fun isRegionalForm(card: CardTraits): Boolean {
        val words = card.name.normalized().split(' ')
        return card.subtypes.any { it.normalized() in regionalTerms } ||
            regionalTerms.any { it in words }
    }

    /** Lower-cases and strips accents so "Pokémon" == "pokemon" and "TAG TEAM" == "tag team". */
    private fun String.normalized(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
}
