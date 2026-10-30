package com.potentplum.dextracker.data

import androidx.room.withTransaction
import com.potentplum.dextracker.data.local.AppDatabase
import com.potentplum.dextracker.data.local.CardEntity
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.local.OwnedCopyEntity
import com.potentplum.dextracker.data.remote.CardDto
import com.potentplum.dextracker.data.remote.TcgApiService
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardPrices
import com.potentplum.dextracker.domain.CardRules
import com.potentplum.dextracker.domain.CardTraits
import com.potentplum.dextracker.domain.CardVariant
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

/** Identifies one line of owned copies: a card in a given variant, condition and language. */
data class CopyKey(
    val cardId: String,
    val variant: CardVariant,
    val condition: CardCondition,
    val language: CardLanguage,
)

class CardRepository(
    private val api: TcgApiService,
    private val database: AppDatabase,
) {
    private val dao = database.collectionDao()

    /** Search results are live queries, but kept for the session so going back is instant. */
    private val sessionCache = mutableMapOf<Int, List<TcgCard>>()
    private val cacheLock = Mutex()

    fun observeOwned(dex: Int): Flow<List<CardWithCopies>> = dao.observeForDex(dex)

    fun observeAllOwned(): Flow<List<CardWithCopies>> = dao.observeAll()

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
            .mapNotNull { it.toDomain(regionalForm = CardRules.isRegionalForm(it.traits())) }
        cacheLock.withLock { sessionCache[dex] = cards }
        return cards
    }

    /** Art of every owned card, to keep it available offline. */
    suspend fun ownedImageUrls(): List<String> = dao.getAll().flatMap { listOf(it.card.imageSmall, it.card.imageLarge) }

    /** Adds one copy, raising the quantity if an identical copy is already owned. */
    suspend fun addCopy(card: CardEntity, key: CopyKey): AddResult = database.withTransaction {
        val newCatch = dao.countCopyRowsForDex(card.dex) == 0
        saveCard(card)
        val existing = dao.findCopy(card.cardId, key.variant.key, key.condition.code, key.language.code)
        if (existing != null) {
            dao.setQuantity(existing.id, existing.quantity + 1)
        } else {
            dao.insertCopy(
                OwnedCopyEntity(
                    cardId = card.cardId,
                    variant = key.variant.key,
                    condition = key.condition.code,
                    language = key.language.code,
                    quantity = 1,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
        val generation = Generation.forDex(card.dex)
        val completed = newCatch &&
            dao.countCaughtBetween(generation.dexRange.first, generation.dexRange.last) >= generation.size
        AddResult(newCatch, if (completed) generation else null)
    }

    /** Removes one copy matching [key], e.g. to undo an add. */
    suspend fun removeOneCopy(key: CopyKey) = database.withTransaction {
        val copy = dao.findCopy(key.cardId, key.variant.key, key.condition.code, key.language.code)
            ?: return@withTransaction
        changeQuantityLocked(copy, -1)
    }

    /** Adds [delta] copies to a line; dropping to zero removes it, and the card with its last copy. */
    suspend fun changeQuantity(copyId: Long, delta: Int) = database.withTransaction {
        val copy = dao.getCopy(copyId) ?: return@withTransaction
        changeQuantityLocked(copy, delta)
    }

    /**
     * Fetches current Cardmarket prices for every owned card. Returns how many were updated;
     * throws if the card database can't be reached.
     */
    suspend fun refreshPrices(): Int {
        val now = System.currentTimeMillis()
        var updated = 0
        dao.ownedCardIds().chunked(PRICE_BATCH).forEach { ids ->
            val query = ids.joinToString(" OR ", prefix = "(", postfix = ")") { "id:\"$it\"" }
            val result = api.searchCards(query = query, select = TcgApiService.PRICE_FIELDS, orderBy = "id")
            database.withTransaction {
                result.data.forEach { dto ->
                    val prices = dto.cardmarket?.prices
                    dao.updatePrices(dto.id, prices?.trendPrice, prices?.reverseHoloTrend, now)
                    updated++
                }
            }
        }
        return updated
    }

    private suspend fun saveCard(card: CardEntity) {
        if (dao.insertCardIfAbsent(card) != -1L) return
        // Already stored: refresh what may have changed since, keeping known prices if the
        // new data has none.
        dao.refreshCardInfo(card.cardId, card.name, card.rarity, card.imageSmall, card.imageLarge, card.variants)
        if (card.pricesUpdatedAt != null) {
            dao.updatePrices(card.cardId, card.priceTrend, card.priceReverseTrend, card.pricesUpdatedAt)
        }
    }

    private suspend fun changeQuantityLocked(copy: OwnedCopyEntity, delta: Int) {
        val quantity = copy.quantity + delta
        if (quantity > 0) {
            dao.setQuantity(copy.id, quantity)
            return
        }
        dao.deleteCopy(copy.id)
        if (dao.countCopyRows(copy.cardId) == 0) dao.deleteCard(copy.cardId)
    }

    private fun CardDto.traits() = CardTraits(name, supertype, subtypes)

    private fun CardDto.toDomain(regionalForm: Boolean): TcgCard? {
        val set = set ?: return null
        val images = images ?: return null
        return TcgCard(
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
            variants = CardVariant.parse(tcgplayer?.prices?.keys.orEmpty()),
            prices = CardPrices(cardmarket?.prices?.trendPrice, cardmarket?.prices?.reverseHoloTrend),
        )
    }

    private companion object {
        /** 250 per page; the most printed Pokémon (Pikachu) is ~210 cards today. */
        const val MAX_PAGES = 4

        /** Card ids per price query, keeping the URL comfortably short. */
        const val PRICE_BATCH = 40
    }
}

/** The stored form of a search result, for saving it with its first copy. */
fun TcgCard.toEntity(dex: Int): CardEntity {
    val hasPrices = prices.trend != null || prices.reverseHoloTrend != null
    return CardEntity(
        cardId = id,
        dex = dex,
        name = name,
        setId = setId,
        setName = setName,
        setSeries = setSeries,
        releaseDate = releaseDate,
        number = number,
        rarity = rarity,
        imageSmall = imageSmall,
        imageLarge = imageLarge,
        variants = variants.joinToString(",") { it.key },
        priceTrend = prices.trend,
        priceReverseTrend = prices.reverseHoloTrend,
        pricesUpdatedAt = if (hasPrices) System.currentTimeMillis() else null,
    )
}

/** Domain views of the stored card. */
val CardEntity.variantList: List<CardVariant>
    get() = CardVariant.parse(variants.split(',').filter { it.isNotBlank() })

val CardEntity.prices: CardPrices get() = CardPrices(priceTrend, priceReverseTrend)
