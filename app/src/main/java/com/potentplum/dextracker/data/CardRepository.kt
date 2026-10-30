package com.potentplum.dextracker.data

import com.potentplum.dextracker.data.local.OwnedCardDao
import com.potentplum.dextracker.data.local.OwnedCardEntity
import com.potentplum.dextracker.data.remote.CardDto
import com.potentplum.dextracker.data.remote.TcgApiService
import com.potentplum.dextracker.domain.CardRules
import com.potentplum.dextracker.domain.CardTraits
import com.potentplum.dextracker.domain.Generation
import com.potentplum.dextracker.domain.TcgCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What adding a card meant for the collection, so the UI can celebrate the right moments. */
data class AddResult(
    /** The first card for this Pokémon: it's now caught. */
    val newCatch: Boolean,
    /** Set when this catch completed the Pokémon's generation. */
    val completedGeneration: Generation?,
)

class CardRepository(
    private val api: TcgApiService,
    private val dao: OwnedCardDao,
) {
    /** Search results are live queries, but kept for the session so going back is instant. */
    private val sessionCache = mutableMapOf<Int, List<TcgCard>>()
    private val cacheLock = Mutex()

    fun observeOwned(dex: Int): Flow<List<OwnedCardEntity>> = dao.observeForDex(dex)

    fun observeOwnedIds(dex: Int): Flow<List<String>> = dao.observeIdsForDex(dex)

    /**
     * Every valid card printed for a Dex entry. Queries by National Dex number rather than
     * name so "Charizard ex", "Dark Charizard" etc. are included and names with punctuation
     * (Farfetch'd, Mr. Mime, Nidoran♀) need no escaping. The rules engine then drops TAG TEAMs
     * and non-Pokémon supertypes, and flags regional forms so callers can apply the user's
     * setting without a new query.
     */
    suspend fun cardsFor(dex: Int, forceRefresh: Boolean = false): List<TcgCard> {
        if (!forceRefresh) cacheLock.withLock { sessionCache[dex] }?.let { return it }

        val query = "nationalPokedexNumbers:$dex supertype:pokemon"
        val all = mutableListOf<CardDto>()
        var page = 1
        while (page <= MAX_PAGES) {
            val result = api.searchCards(query = query, page = page)
            all += result.data
            if (result.data.isEmpty() || all.size >= result.totalCount) break
            page++
        }

        val cards = all
            .filter { CardRules.isAllowed(it.traits()) }
            .distinctBy { it.id }
            .map { it.toDomain(regionalForm = CardRules.isRegionalForm(it.traits())) }
        cacheLock.withLock { sessionCache[dex] = cards }
        return cards
    }

    suspend fun isOwned(cardId: String): Boolean = dao.get(cardId) != null

    suspend fun addToCollection(dex: Int, card: TcgCard): AddResult {
        val newCatch = dao.countForDex(dex) == 0
        dao.insert(
            OwnedCardEntity(
                cardId = card.id,
                dex = dex,
                name = card.name,
                setId = card.setId,
                setName = card.setName,
                setSeries = card.setSeries,
                releaseDate = card.releaseDate,
                number = card.number,
                rarity = card.rarity,
                imageSmall = card.imageSmall,
                imageLarge = card.imageLarge,
                addedAt = System.currentTimeMillis(),
            )
        )
        val generation = Generation.forDex(dex)
        val completed = newCatch &&
            dao.countCaughtBetween(generation.dexRange.first, generation.dexRange.last) >= generation.size
        return AddResult(newCatch, if (completed) generation else null)
    }

    /** Removes a card and returns what was stored, so the removal can be undone. */
    suspend fun removeFromCollection(cardId: String): OwnedCardEntity? {
        val removed = dao.get(cardId)
        dao.delete(cardId)
        return removed
    }

    /** Puts back a card removed with [removeFromCollection], keeping its original date. */
    suspend fun restore(card: OwnedCardEntity) = dao.insert(card)

    private fun CardDto.traits() = CardTraits(name, supertype, subtypes)

    private fun CardDto.toDomain(regionalForm: Boolean) = TcgCard(
        id = id,
        name = name,
        number = number,
        rarity = rarity,
        setId = set.id,
        setName = set.name,
        setSeries = set.series,
        releaseDate = set.releaseDate,
        imageSmall = images.small,
        imageLarge = images.large,
        regionalForm = regionalForm,
    )

    private companion object {
        /** 250 per page; the most printed Pokémon (Pikachu) is ~210 cards today. */
        const val MAX_PAGES = 4
    }
}
