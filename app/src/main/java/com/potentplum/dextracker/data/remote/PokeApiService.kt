package com.potentplum.dextracker.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** https://pokeapi.co — free, no key required. */
interface PokeApiService {
    @GET("pokemon-species")
    suspend fun speciesList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0,
    ): NamedResourceList

    @GET("pokemon-species/{id}")
    suspend fun species(@Path("id") id: Int): SpeciesDto

    @GET("pokemon/{id}")
    suspend fun pokemon(@Path("id") id: Int): PokemonDto

    companion object {
        const val BASE_URL = "https://pokeapi.co/api/v2/"

        fun spriteUrl(dex: Int) =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$dex.png"
    }
}

@Serializable
data class NamedResourceList(val count: Int, val results: List<NamedResource>)

@Serializable
data class NamedResource(val name: String, val url: String) {
    /** PokeAPI URLs end in ".../{id}/". */
    val id: Int get() = url.trimEnd('/').substringAfterLast('/').toInt()
}

@Serializable
data class SpeciesDto(
    val id: Int,
    val names: List<LocalizedName> = emptyList(),
    val genera: List<Genus> = emptyList(),
    @SerialName("flavor_text_entries") val flavorTextEntries: List<FlavorText> = emptyList(),
)

@Serializable
data class LocalizedName(val name: String, val language: NamedResource)

@Serializable
data class Genus(val genus: String, val language: NamedResource)

@Serializable
data class FlavorText(
    @SerialName("flavor_text") val flavorText: String,
    val language: NamedResource,
)

@Serializable
data class PokemonDto(
    val id: Int,
    /** Decimetres. */
    val height: Int,
    /** Hectograms. */
    val weight: Int,
    val types: List<PokemonTypeSlot>,
)

@Serializable
data class PokemonTypeSlot(val slot: Int, val type: NamedResource)
