package com.potentplum.dextracker.data

import androidx.room.withTransaction
import com.potentplum.dextracker.data.local.AppDatabase
import com.potentplum.dextracker.data.local.CardEntity
import com.potentplum.dextracker.data.local.WantedCardEntity
import com.potentplum.dextracker.data.remote.TcgApiService
import com.potentplum.dextracker.domain.CardPrices
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.TcgCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The wanted list: specific cards the user is looking for. */
class WantedRepository(
    private val api: TcgApiService,
    private val database: AppDatabase,
) {
    private val dao = database.wantedDao()

    /** Newest first. */
    fun observeAll(): Flow<List<WantedCardEntity>> = dao.observeAll()

    fun observeIds(): Flow<Set<String>> = dao.observeAll().map { list -> list.mapTo(HashSet()) { it.cardId } }

    /** Pokédex entries with at least one wanted card. */
    fun observeDexes(): Flow<Set<Int>> =
        dao.observeAll().map { list -> list.mapNotNullTo(HashSet()) { it.dex.takeIf { dex -> dex > 0 } } }

    /** Puts a card on the list, or takes it off if it's already there. */
    suspend fun toggle(card: TcgCard, dex: Int) = database.withTransaction {
        if (dao.delete(card.id) == 0) dao.upsert(card.toWanted(dex))
    }

    /** Takes a card off the list, returning it so it can be put back. */
    suspend fun remove(cardId: String): WantedCardEntity? = database.withTransaction {
        dao.get(cardId)?.also { dao.delete(cardId) }
    }

    /** Puts back a card taken off the list, e.g. to undo. */
    suspend fun restore(card: WantedCardEntity) = dao.upsert(card)

    /** Fetches current Cardmarket prices for the wanted cards; throws if offline. */
    suspend fun refreshPrices() {
        val now = System.currentTimeMillis()
        dao.getAll().map { it.cardId }.chunked(PRICE_BATCH).forEach { ids ->
            val query = ids.joinToString(" OR ", prefix = "(", postfix = ")") { "id:\"$it\"" }
            val result = api.searchCards(query = query, select = TcgApiService.PRICE_FIELDS, orderBy = "id")
            database.withTransaction {
                result.data.forEach { dto ->
                    val prices = dto.cardmarket?.prices
                    dao.updatePrices(dto.id, prices?.trendPrice, prices?.reverseHoloTrend, now)
                }
            }
        }
    }

    private companion object {
        const val PRICE_BATCH = 40
    }
}

fun TcgCard.toWanted(dex: Int): WantedCardEntity {
    val hasPrices = prices.trend != null || prices.reverseHoloTrend != null
    return WantedCardEntity(
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
        addedAt = System.currentTimeMillis(),
    )
}

/** The card as it would be stored on getting a copy of it. */
fun WantedCardEntity.toCardEntity() = CardEntity(
    cardId = cardId,
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
    variants = variants,
    priceTrend = priceTrend,
    priceReverseTrend = priceReverseTrend,
    pricesUpdatedAt = pricesUpdatedAt,
)

val WantedCardEntity.variantList: List<CardVariant>
    get() = CardVariant.parse(variants.split(',').filter { it.isNotBlank() })

/** What one copy costs in the variant most people buy. */
val WantedCardEntity.price: Double?
    get() = CardPrices(priceTrend, priceReverseTrend).unitValue(CardVariant.defaultFor(variantList))
