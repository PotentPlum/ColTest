package com.potentplum.dextracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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

/**
 * A TCG card that has at least one owned copy. Holds what the app shows about the card, plus
 * its last known Cardmarket prices so values work offline.
 */
@Entity(tableName = "card", indices = [Index("dex")])
data class CardEntity(
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
    /** Comma separated variant keys the card was printed in; empty when unknown. */
    val variants: String,
    /** Cardmarket trend price in euros. */
    val priceTrend: Double?,
    /** Cardmarket reverse holo trend price in euros. */
    val priceReverseTrend: Double?,
    /** When the prices were fetched (epoch millis); null if never. */
    val pricesUpdatedAt: Long?,
)

/**
 * Copies of a card the user owns in one variant, condition and language. Adding another
 * identical copy raises [quantity] rather than adding a row.
 */
@Entity(
    tableName = "owned_copy",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["cardId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["cardId", "variant", "condition", "language"], unique = true)],
)
data class OwnedCopyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    /** A [com.potentplum.dextracker.domain.CardVariant] key. */
    val variant: String,
    /** A [com.potentplum.dextracker.domain.CardCondition] code. */
    val condition: String,
    /** A [com.potentplum.dextracker.domain.CardLanguage] code. */
    val language: String,
    val quantity: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

/**
 * A card on the wanted list. Keeps its own copy of the card's details, since a wanted card
 * usually isn't owned yet and so has no row in `card`.
 */
@Entity(tableName = "wanted_card")
data class WantedCardEntity(
    @PrimaryKey val cardId: String,
    /** The Pokédex entry the card counts toward, or 0 for cards that don't count. */
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
    /** Comma separated variant keys the card was printed in; empty when unknown. */
    val variants: String,
    val priceTrend: Double?,
    val priceReverseTrend: Double?,
    val pricesUpdatedAt: Long?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

/** How many cards of a set are owned, for the Sets tab. */
data class SetOwnedCount(val setId: String, val owned: Int)

/** A card with all the copies the user owns of it. */
data class CardWithCopies(
    @Embedded val card: CardEntity,
    @Relation(parentColumn = "cardId", entityColumn = "cardId")
    val copies: List<OwnedCopyEntity>,
)

/** Projection used by the grid: a Pokémon plus how many unique card variants are owned. */
data class PokemonWithCount(
    val dex: Int,
    val name: String,
    val generation: Int,
    val cardCount: Int,
)
