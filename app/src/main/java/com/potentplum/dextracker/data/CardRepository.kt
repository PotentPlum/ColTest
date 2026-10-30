package com.potentplum.dextracker.data

import com.potentplum.dextracker.data.local.OwnedCardDao
import com.potentplum.dextracker.data.local.OwnedCardEntity
import com.potentplum.dextracker.data.remote.CardDto
import com.potentplum.dextracker.data.remote.TcgApiService
import com.potentplum.dextracker.domain.CardRules
import com.potentplum.dextracker.domain.CardTraits
import com.potentplum.dextracker.domain.TcgCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
     * (Farfetch'd, Mr. Mime, Nidoran♀) need no escaping. The rules engine then drops
     * regional forms, TAG TEAMs and non-Pokémon supertypes.
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
            .filter { CardRules.isAllowed(CardTraits(it.name, it.supertype, it.subtypes)) }
            .distinctBy { it.id }
            .map { it.toDomain() }
        cacheLock.withLock { sessionCache[dex] = cards }
        return cards
    }

    suspend fun addToCollection(dex: Int, card: TcgCard) {
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
    }

    suspend fun removeFromCollection(cardId: String) = dao.delete(cardId)

    private fun CardDto.toDomain() = TcgCard(
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
    )

    private companion object {
        /** 250 per page; the most printed Pokémon (Pikachu) is ~210 cards today. */
        const val MAX_PAGES = 4
    }
}
