package com.potentplum.dextracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per National Dex entry, cached from PokeAPI on launch. */
@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val dex: Int,
    val name: String,
    val generation: Int,
)

/** Detail data for the Pokédex page, cached the first time a page is opened. */
@Entity(tableName = "pokemon_detail")
data class PokemonDetailEntity(
    @PrimaryKey val dex: Int,
    val displayName: String,
    val genus: String,
    /** Comma separated, slot order, e.g. "grass,poison". */
    val types: String,
    val heightDecimetres: Int,
    val weightHectograms: Int,
    val flavorText: String,
)

/** A card the user owns. The card id is the TCG variant id, so each variant is logged once. */
@Entity(tableName = "owned_card", indices = [Index("dex")])
data class OwnedCardEntity(
    @PrimaryKey val cardId: String,
    val dex: Int,
    val name: String,
    val setId: String,
    val setName: String,
    val setSeries: String,
    val releaseDate: String,
    val number: String,
    val rarity: String?,
    val imageSmall: String,
    val imageLarge: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

/** Projection used by the grid: a Pokémon plus how many unique variants are owned. */
data class PokemonWithCount(
    val dex: Int,
    val name: String,
    val generation: Int,
    val cardCount: Int,
)
