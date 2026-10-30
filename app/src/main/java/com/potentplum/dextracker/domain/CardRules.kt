package com.potentplum.dextracker.domain

import java.text.Normalizer

/** The fields of a TCG card the rules engine needs to make a decision. */
data class CardTraits(
    val name: String,
    val supertype: String,
    val subtypes: List<String>,
)

/**
 * Strict rules engine for TCG search results. Anything matched here is dropped before the
 * user ever sees it, regardless of what the API returned.
 */
object CardRules {
    /** Regional variants are tracked as separate forms, not as the base Pokémon. */
    private val excludedNameTerms = listOf("alolan", "galarian")

    /** Multi-Pokémon cards don't belong to a single Dex entry. */
    private val excludedSubtypes = listOf("tag team", "alolan", "galarian")

    fun isAllowed(card: CardTraits): Boolean {
        // Only Pokémon cards are collectible against a Dex entry; Trainer and Energy are dropped.
        if (card.supertype.normalized() != "pokemon") return false

        val subtypes = card.subtypes.map { it.normalized() }
        if (subtypes.any { it in excludedSubtypes }) return false

        val name = card.name.normalized()
        return excludedNameTerms.none { term -> name.split(' ').any { it == term } }
    }

    /** Lower-cases and strips accents so "Pokémon" == "pokemon" and "TAG TEAM" == "tag team". */
    private fun String.normalized(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
}
