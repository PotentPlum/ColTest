package com.potentplum.dextracker.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * https://pokemontcg.io v2 — free and usable without an API key (keyless requests are
 * rate limited, which is plenty for one person adding cards by hand).
 */
interface TcgApiService {
    @GET("cards")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
        @Query("orderBy") orderBy: String = "set.releaseDate",
        @Query("select") select: String = SELECT_FIELDS,
    ): CardPage

    companion object {
        const val BASE_URL = "https://api.pokemontcg.io/v2/"
        const val MAX_PAGE_SIZE = 250
        private const val SELECT_FIELDS =
            "id,name,supertype,subtypes,number,rarity,set,images,nationalPokedexNumbers"
    }
}

@Serializable
data class CardPage(
    val data: List<CardDto>,
    val page: Int = 1,
    val pageSize: Int = 0,
    val count: Int = 0,
    val totalCount: Int = 0,
)

@Serializable
data class CardDto(
    val id: String,
    val name: String,
    val supertype: String = "",
    val subtypes: List<String> = emptyList(),
    val number: String = "",
    val rarity: String? = null,
    val set: CardSetDto,
    val images: CardImagesDto,
    val nationalPokedexNumbers: List<Int> = emptyList(),
)

@Serializable
data class CardSetDto(
    val id: String,
    val name: String,
    val series: String = "",
    /** Formatted "yyyy/MM/dd". */
    val releaseDate: String = "",
)

@Serializable
data class CardImagesDto(val small: String, val large: String)
