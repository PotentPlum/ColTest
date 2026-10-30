package com.potentplum.dextracker.domain

/** A card returned by the TCG search that survived the rules engine. */
data class TcgCard(
    val id: String,
    val name: String,
    val number: String,
    val rarity: String?,
    val setId: String,
    val setName: String,
    val setSeries: String,
    /** "yyyy/MM/dd" as returned by the API; sorts chronologically as a string. */
    val releaseDate: String,
    val imageSmall: String,
    val imageLarge: String,
    /** Alolan, Galarian, Hisuian or Paldean form; only shown when the user counts them. */
    val regionalForm: Boolean = false,
) {
    val releaseYear: String get() = releaseDate.take(4)
}
