package com.potentplum.dextracker.data.backup

import com.potentplum.dextracker.data.local.CardEntity
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.local.OwnedCopyEntity
import com.potentplum.dextracker.domain.CardVariant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The collection as written to a backup file. Field names are the file format; keep them stable. */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val includeRegionalForms: Boolean = false,
    val cards: List<BackupCard>,
)

@Serializable
data class BackupCard(
    val cardId: String,
    val dex: Int,
    val name: String,
    val setId: String,
    val setName: String,
    val setSeries: String,
    val releaseDate: String,
    val number: String,
    val rarity: String? = null,
    val imageSmall: String,
    val imageLarge: String,
    /** Version 1 only: when the card was logged. Version 2 keeps this per copy. */
    val addedAt: Long? = null,
    /** Version 2: variant keys the card was printed in. */
    val variants: List<String> = emptyList(),
    /** Version 2: the copies owned. Missing in version 1, where each card was one copy. */
    val copies: List<BackupCopy>? = null,
)

@Serializable
data class BackupCopy(
    val variant: String,
    val condition: String,
    val language: String,
    val quantity: Int,
    val addedAt: Long,
)

data class DecodedBackup(val cards: List<CardWithCopies>, val includeRegionalForms: Boolean)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Converts the collection to and from the JSON backup format. Reads versions 1 and 2. */
object BackupCodec {
    const val FORMAT = "pokedex-tracker-backup"
    const val VERSION = 2

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(cards: List<CardWithCopies>, includeRegionalForms: Boolean, exportedAt: Long): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                format = FORMAT,
                version = VERSION,
                exportedAt = exportedAt,
                includeRegionalForms = includeRegionalForms,
                cards = cards.map { it.toBackup() },
            ),
        )

    fun decode(text: String): DecodedBackup {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not a Pokédex Tracker backup", e)
        }
        if (file.format != FORMAT) throw InvalidBackupException("Not a Pokédex Tracker backup")
        if (file.version > VERSION) throw InvalidBackupException("Backup is from a newer version of the app")
        return DecodedBackup(file.cards.map { it.toEntity() }, file.includeRegionalForms)
    }

    private fun CardWithCopies.toBackup() = BackupCard(
        cardId = card.cardId,
        dex = card.dex,
        name = card.name,
        setId = card.setId,
        setName = card.setName,
        setSeries = card.setSeries,
        releaseDate = card.releaseDate,
        number = card.number,
        rarity = card.rarity,
        imageSmall = card.imageSmall,
        imageLarge = card.imageLarge,
        variants = card.variants.split(',').filter { it.isNotBlank() },
        copies = copies.map { BackupCopy(it.variant, it.condition, it.language, it.quantity, it.addedAt) },
    )

    private fun BackupCard.toEntity(): CardWithCopies {
        val card = CardEntity(
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
            variants = variants.joinToString(","),
            // Prices aren't backed up; they're refreshed from Cardmarket.
            priceTrend = null,
            priceReverseTrend = null,
            pricesUpdatedAt = null,
        )
        val ownedCopies = copies
            ?.filter { it.quantity > 0 }
            ?.map { OwnedCopyEntity(0, cardId, it.variant, it.condition, it.language, it.quantity, it.addedAt) }
            // Version 1: one copy per card, logged before variants were tracked.
            ?: listOf(OwnedCopyEntity(0, cardId, CardVariant.UNSPECIFIED.key, "NM", "EN", 1, addedAt ?: 0L))
        return CardWithCopies(card, ownedCopies)
    }
}
