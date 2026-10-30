package com.potentplum.dextracker.data

import com.potentplum.dextracker.data.local.PokemonDao
import com.potentplum.dextracker.data.local.PokemonDetailEntity
import com.potentplum.dextracker.data.local.PokemonEntity
import com.potentplum.dextracker.data.local.PokemonWithCount
import com.potentplum.dextracker.data.remote.PokeApiService
import com.potentplum.dextracker.data.remote.PokemonDto
import com.potentplum.dextracker.data.remote.SpeciesDto
import com.potentplum.dextracker.domain.Generation
import com.potentplum.dextracker.domain.NameFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class PokedexRepository(
    private val api: PokeApiService,
    private val dao: PokemonDao,
) {
    fun observeGeneration(generation: Generation): Flow<List<PokemonWithCount>> =
        dao.observeGeneration(generation.number)

    /** Caches the Gen 1–3 species list once; afterwards the grid works fully offline. */
    suspend fun ensureSpeciesCached() {
        if (dao.count() >= Generation.MAX_DEX) return
        val species = api.speciesList(limit = Generation.MAX_DEX).results
        dao.upsertAll(
            species
                .filter { it.id <= Generation.MAX_DEX }
                .map {
                    PokemonEntity(
                        dex = it.id,
                        name = NameFormatter.fromSlug(it.name),
                        generation = Generation.forDex(it.id).number,
                    )
                }
        )
    }

    suspend fun cachedName(dex: Int): String? = dao.get(dex)?.name

    /** Returns cached detail if present, otherwise fetches it from PokeAPI and caches it. */
    suspend fun detail(dex: Int): PokemonDetailEntity {
        dao.getDetail(dex)?.let { return it }
        val detail = coroutineScope {
            val species = async { api.species(dex) }
            val pokemon = async { api.pokemon(dex) }
            toDetail(species.await(), pokemon.await(), fallbackName = cachedName(dex).orEmpty())
        }
        dao.upsertDetail(detail)
        return detail
    }

    private fun toDetail(species: SpeciesDto, pokemon: PokemonDto, fallbackName: String) =
        PokemonDetailEntity(
            dex = species.id,
            displayName = species.names.firstOrNull { it.language.name == "en" }?.name
                ?: fallbackName,
            genus = species.genera.firstOrNull { it.language.name == "en" }?.genus.orEmpty(),
            types = pokemon.types.sortedBy { it.slot }.joinToString(",") { it.type.name },
            heightDecimetres = pokemon.height,
            weightHectograms = pokemon.weight,
            flavorText = species.flavorTextEntries
                .firstOrNull { it.language.name == "en" }
                ?.flavorText
                ?.let(::cleanFlavorText)
                .orEmpty(),
        )

    /** Game text contains form feeds, hard line breaks and soft hyphens from the cartridge. */
    private fun cleanFlavorText(raw: String): String =
        raw.replace("\u00AD\n", "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
