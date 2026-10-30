package com.potentplum.dextracker.data.backup

import com.potentplum.dextracker.data.local.OwnedCardEntity
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
    val addedAt: Long,
)

data class DecodedBackup(val cards: List<OwnedCardEntity>, val includeRegionalForms: Boolean)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Converts the collection to and from the JSON backup format. */
object BackupCodec {
    const val FORMAT = "pokedex-tracker-backup"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(cards: List<OwnedCardEntity>, includeRegionalForms: Boolean, exportedAt: Long): String =
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

    private fun OwnedCardEntity.toBackup() = BackupCard(
        cardId, dex, name, setId, setName, setSeries, releaseDate, number, rarity, imageSmall, imageLarge, addedAt,
    )

    private fun BackupCard.toEntity() = OwnedCardEntity(
        cardId, dex, name, setId, setName, setSeries, releaseDate, number, rarity, imageSmall, imageLarge, addedAt,
    )
}
