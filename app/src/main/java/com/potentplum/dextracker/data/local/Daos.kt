package com.potentplum.dextracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonDao {
    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(pokemon: List<PokemonEntity>)

    /** The whole National Dex with card counts; the grid filters it in memory. */
    @Query(
        """
        SELECT p.dex, p.name, p.generation, COUNT(c.cardId) AS cardCount
        FROM pokemon p LEFT JOIN owned_card c ON c.dex = p.dex
        GROUP BY p.dex
        ORDER BY p.dex
        """
    )
    fun observeAll(): Flow<List<PokemonWithCount>>

    @Query("SELECT * FROM pokemon WHERE dex = :dex")
    suspend fun get(dex: Int): PokemonEntity?

    @Query("SELECT * FROM pokemon_detail WHERE dex = :dex")
    suspend fun getDetail(dex: Int): PokemonDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: PokemonDetailEntity)
}

@Dao
interface OwnedCardDao {
    @Query("SELECT * FROM owned_card WHERE dex = :dex ORDER BY releaseDate, number")
    fun observeForDex(dex: Int): Flow<List<OwnedCardEntity>>

    @Query("SELECT cardId FROM owned_card WHERE dex = :dex")
    fun observeIdsForDex(dex: Int): Flow<List<String>>

    @Query("SELECT * FROM owned_card ORDER BY dex, releaseDate, number")
    suspend fun getAll(): List<OwnedCardEntity>

    @Query("SELECT * FROM owned_card WHERE cardId = :cardId")
    suspend fun get(cardId: String): OwnedCardEntity?

    @Query("SELECT COUNT(*) FROM owned_card WHERE dex = :dex")
    suspend fun countForDex(dex: Int): Int

    /** How many distinct Pokémon in a Dex range have at least one card. */
    @Query("SELECT COUNT(DISTINCT dex) FROM owned_card WHERE dex BETWEEN :first AND :last")
    suspend fun countCaughtBetween(first: Int, last: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: OwnedCardEntity)

    /** Returns the row id per card, or -1 for cards that were already owned. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<OwnedCardEntity>): List<Long>

    @Query("DELETE FROM owned_card WHERE cardId = :cardId")
    suspend fun delete(cardId: String)
}
