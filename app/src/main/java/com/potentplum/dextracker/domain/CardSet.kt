package com.potentplum.dextracker.domain

/** A TCG expansion or promo series, as listed on the Sets tab. */
data class CardSet(
    val id: String,
    val name: String,
    val series: String,
    /** "yyyy/MM/dd" as returned by the API; sorts chronologically as a string. */
    val releaseDate: String,
    /** The number printed on the cards, e.g. the 128 in "132/128". */
    val printedTotal: Int,
    /** Every card in the set, secret rares included. */
    val total: Int,
    /** Short code printed on the cards, e.g. "30C"; null for older sets. */
    val code: String?,
    val logoUrl: String,
    val symbolUrl: String,
) {
    val releaseYear: String get() = releaseDate.take(4)

    fun matches(query: String): Boolean =
        listOf(name, series, id, code.orEmpty(), releaseYear).any { it.contains(query, ignoreCase = true) }
}

/**
 * A card in a set. [dex] is the Pokédex entry it counts toward, or 0 for cards that don't
 * count: Trainers, Energy and multi-Pokémon cards.
 */
data class SetCard(val card: TcgCard, val dex: Int) {
    val countsTowardDex: Boolean get() = dex > 0
}

/** Orders card numbers the way they're printed: 2 before 10, and "TG05" after the main set. */
object CardNumberOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val ka = key(a)
        val kb = key(b)
        return compareValuesBy(ka, kb, { it.prefix }, { it.value }, { it.raw })
    }

    private data class Key(val prefix: String, val value: Long, val raw: String)

    /** "TG05" → ("TG", 5); "132" → ("", 132); "H03" → ("H", 3); "?" → ("?", MAX). */
    private fun key(number: String): Key {
        val prefix = number.takeWhile { !it.isDigit() }
        val digits = number.drop(prefix.length).takeWhile { it.isDigit() }
        return Key(prefix.uppercase(), digits.take(18).toLongOrNull() ?: Long.MAX_VALUE, number)
    }
}
