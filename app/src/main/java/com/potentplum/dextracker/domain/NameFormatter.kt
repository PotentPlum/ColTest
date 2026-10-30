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
        "mime-jr" to "Mime Jr.",
        "porygon-z" to "Porygon-Z",
        "flabebe" to "Flabébé",
        "type-null" to "Type: Null",
        "jangmo-o" to "Jangmo-o",
        "hakamo-o" to "Hakamo-o",
        "kommo-o" to "Kommo-o",
        "sirfetchd" to "Sirfetch'd",
        "mr-rime" to "Mr. Rime",
        "wo-chien" to "Wo-Chien",
        "chien-pao" to "Chien-Pao",
        "ting-lu" to "Ting-Lu",
        "chi-yu" to "Chi-Yu",
    )

    fun fromSlug(slug: String): String =
        specialCases[slug] ?: slug.split('-').joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercaseChar() }
        }
}
