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

    @Query(
        """
        SELECT p.dex, p.name, p.generation, COUNT(c.cardId) AS cardCount
        FROM pokemon p LEFT JOIN owned_card c ON c.dex = p.dex
        WHERE p.generation = :generation
        GROUP BY p.dex
        ORDER BY p.dex
        """
    )
    fun observeGeneration(generation: Int): Flow<List<PokemonWithCount>>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: OwnedCardEntity)

    @Query("DELETE FROM owned_card WHERE cardId = :cardId")
    suspend fun delete(cardId: String)
}
