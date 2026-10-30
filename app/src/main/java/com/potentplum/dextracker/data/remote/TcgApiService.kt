package com.potentplum.dextracker.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
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

    /** Every set, newest first; there are fewer than one page of them. */
    @GET("sets")
    suspend fun sets(
        @Query("orderBy") orderBy: String = "-releaseDate",
        @Query("pageSize") pageSize: Int = MAX_PAGE_SIZE,
        @Query("select") select: String = SET_FIELDS,
    ): SetPage

    companion object {
        const val BASE_URL = "https://api.pokemontcg.io/v2/"
        const val MAX_PAGE_SIZE = 250
        private const val SELECT_FIELDS =
            "id,name,supertype,subtypes,number,rarity,set,images,nationalPokedexNumbers,tcgplayer,cardmarket"

        private const val SET_FIELDS = "id,name,series,printedTotal,total,ptcgoCode,releaseDate,images"

        /** Just what's needed to refresh prices of cards already in the collection. */
        const val PRICE_FIELDS = "id,cardmarket"
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
    val name: String = "",
    val supertype: String = "",
    val subtypes: List<String> = emptyList(),
    val number: String = "",
    val rarity: String? = null,
    val set: CardSetDto? = null,
    val images: CardImagesDto? = null,
    val nationalPokedexNumbers: List<Int> = emptyList(),
    val tcgplayer: TcgPlayerDto? = null,
    val cardmarket: CardmarketDto? = null,
)

/** Only the keys of [prices] are used: they name the variants a card was printed in. */
@Serializable
data class TcgPlayerDto(val prices: Map<String, JsonElement> = emptyMap())

/** Cardmarket prices in euros. */
@Serializable
data class CardmarketDto(val prices: CardmarketPricesDto? = null)

@Serializable
data class CardmarketPricesDto(
    val trendPrice: Double? = null,
    val reverseHoloTrend: Double? = null,
)

@Serializable
data class SetPage(val data: List<CardSetDto>)

@Serializable
data class CardSetDto(
    val id: String,
    val name: String,
    val series: String = "",
    /** Formatted "yyyy/MM/dd". */
    val releaseDate: String = "",
    /** The number printed on the cards, e.g. the 128 in "132/128". */
    val printedTotal: Int = 0,
    /** Every card in the set, secret rares included. */
    val total: Int = 0,
    /** Short code printed on the cards, e.g. "30C"; missing for older sets. */
    val ptcgoCode: String? = null,
    val images: SetImagesDto? = null,
)

@Serializable
data class SetImagesDto(val symbol: String = "", val logo: String = "")

@Serializable
data class CardImagesDto(val small: String, val large: String)
