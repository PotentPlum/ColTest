package com.potentplum.dextracker.domain

/**
 * PokeAPI list endpoints only return slugs ("mr-mime", "nidoran-f"). This turns them into
 * the names printed in the games so the grid can be labelled without a request per Pokémon.
 */
object NameFormatter {
    private val specialCases = mapOf(
        "nidoran-f" to "Nidoran♀",
        "nidoran-m" to "Nidoran♂",
        "mr-mime" to "Mr. Mime",
        "farfetchd" to "Farfetch'd",
        "ho-oh" to "Ho-Oh",
    )

    fun fromSlug(slug: String): String =
        specialCases[slug] ?: slug.split('-').joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercaseChar() }
        }
}
