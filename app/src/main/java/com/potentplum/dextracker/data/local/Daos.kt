package com.potentplum.dextracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonDao {
    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(pokemon: List<PokemonEntity>)

    /**
     * The whole National Dex with how many unique card variants are owned of each; the grid
     * filters it in memory.
     */
    @Query(
        """
        SELECT p.dex, p.name, p.generation, COALESCE(owned.variants, 0) AS cardCount
        FROM pokemon p
        LEFT JOIN (
            SELECT c.dex AS dex, COUNT(DISTINCT o.cardId || '|' || o.variant) AS variants
            FROM owned_copy o JOIN card c ON c.cardId = o.cardId
            GROUP BY c.dex
        ) owned ON owned.dex = p.dex
        ORDER BY p.dex
        """
    )
    fun observeAll(): Flow<List<PokemonWithCount>>

    @Query("SELECT * FROM pokemon WHERE dex = :dex")
    suspend fun get(dex: Int): PokemonEntity?

    @Query("SELECT * FROM pokemon_detail WHERE dex = :dex")
    suspend fun getDetail(dex: Int): PokemonDetailEntity?

    @Query("SELECT dex FROM pokemon_detail")
    suspend fun cachedDetailDexes(): List<Int>

    @Query("SELECT COUNT(*) FROM pokemon_detail")
    fun observeCachedDetailCount(): Flow<Int>

    @Upsert
    suspend fun upsertDetail(detail: PokemonDetailEntity)
}

@Dao
interface CollectionDao {
    @Transaction
    @Query(
        """
        SELECT * FROM card
        WHERE dex = :dex AND cardId IN (SELECT cardId FROM owned_copy)
        ORDER BY releaseDate, number
        """
    )
    fun observeForDex(dex: Int): Flow<List<CardWithCopies>>

    @Transaction
    @Query("SELECT * FROM card WHERE cardId IN (SELECT cardId FROM owned_copy) ORDER BY dex, releaseDate, number")
    fun observeAll(): Flow<List<CardWithCopies>>

    @Transaction
    @Query("SELECT * FROM card WHERE cardId IN (SELECT cardId FROM owned_copy) ORDER BY dex, releaseDate, number")
    suspend fun getAll(): List<CardWithCopies>

    @Query("SELECT * FROM owned_copy WHERE id = :id")
    suspend fun getCopy(id: Long): OwnedCopyEntity?

    @Query(
        """
        SELECT * FROM owned_copy
        WHERE cardId = :cardId AND variant = :variant AND condition = :condition AND language = :language
        """
    )
    suspend fun findCopy(cardId: String, variant: String, condition: String, language: String): OwnedCopyEntity?

    @Query("SELECT COUNT(*) FROM owned_copy WHERE cardId = :cardId")
    suspend fun countCopyRows(cardId: String): Int

    @Query("SELECT COUNT(*) FROM owned_copy o JOIN card c ON c.cardId = o.cardId WHERE c.dex = :dex")
    suspend fun countCopyRowsForDex(dex: Int): Int

    /** How many distinct Pokémon in a Dex range have at least one card. */
    @Query(
        """
        SELECT COUNT(DISTINCT c.dex) FROM owned_copy o JOIN card c ON c.cardId = o.cardId
        WHERE c.dex BETWEEN :first AND :last
        """
    )
    suspend fun countCaughtBetween(first: Int, last: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCardIfAbsent(card: CardEntity): Long

    @Query(
        """
        UPDATE card SET name = :name, rarity = :rarity, imageSmall = :imageSmall, imageLarge = :imageLarge,
            variants = CASE WHEN :variants = '' THEN variants ELSE :variants END
        WHERE cardId = :cardId
        """
    )
    suspend fun refreshCardInfo(
        cardId: String,
        name: String,
        rarity: String?,
        imageSmall: String,
        imageLarge: String,
        variants: String,
    )

    @Query(
        """
        UPDATE card SET priceTrend = :trend, priceReverseTrend = :reverseTrend, pricesUpdatedAt = :updatedAt
        WHERE cardId = :cardId
        """
    )
    suspend fun updatePrices(cardId: String, trend: Double?, reverseTrend: Double?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCopy(copy: OwnedCopyEntity): Long

    /** Returns the row id per copy, or -1 for identical copies that were already owned. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCopiesIfAbsent(copies: List<OwnedCopyEntity>): List<Long>

    @Query("UPDATE owned_copy SET quantity = :quantity WHERE id = :id")
    suspend fun setQuantity(id: Long, quantity: Int)

    @Query("UPDATE owned_copy SET variant = :variant, condition = :condition, language = :language WHERE id = :id")
    suspend fun setCopyDetails(id: Long, variant: String, condition: String, language: String)

    @Query("DELETE FROM owned_copy WHERE id = :id")
    suspend fun deleteCopy(id: Long)

    @Query("DELETE FROM card WHERE cardId = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("SELECT cardId FROM card WHERE cardId IN (SELECT cardId FROM owned_copy)")
    suspend fun ownedCardIds(): List<String>

    @Transaction
    @Query("SELECT * FROM card WHERE setId = :setId AND cardId IN (SELECT cardId FROM owned_copy)")
    fun observeOwnedInSet(setId: String): Flow<List<CardWithCopies>>

    @Query(
        """
        SELECT setId, COUNT(*) AS owned FROM card
        WHERE cardId IN (SELECT cardId FROM owned_copy)
        GROUP BY setId
        """
    )
    fun observeOwnedCountsBySet(): Flow<List<SetOwnedCount>>

    /** Pokédex entries with at least one card. */
    @Query("SELECT DISTINCT c.dex FROM owned_copy o JOIN card c ON c.cardId = o.cardId WHERE c.dex > 0")
    fun observeCaughtDexes(): Flow<List<Int>>
}

@Dao
interface WantedDao {
    @Query("SELECT * FROM wanted_card ORDER BY added_at DESC")
    fun observeAll(): Flow<List<WantedCardEntity>>

    @Query("SELECT * FROM wanted_card ORDER BY added_at")
    suspend fun getAll(): List<WantedCardEntity>

    @Query("SELECT * FROM wanted_card WHERE cardId = :cardId")
    suspend fun get(cardId: String): WantedCardEntity?

    @Upsert
    suspend fun upsert(card: WantedCardEntity)

    /** Returns the row id per card, or -1 for cards already on the list. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(cards: List<WantedCardEntity>): List<Long>

    @Query("DELETE FROM wanted_card WHERE cardId = :cardId")
    suspend fun delete(cardId: String): Int

    @Query(
        """
        UPDATE wanted_card SET priceTrend = :trend, priceReverseTrend = :reverseTrend, pricesUpdatedAt = :updatedAt
        WHERE cardId = :cardId
        """
    )
    suspend fun updatePrices(cardId: String, trend: Double?, reverseTrend: Double?, updatedAt: Long)
}
