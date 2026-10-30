package com.potentplum.dextracker.data.backup

import android.content.Context
import android.net.Uri
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.local.OwnedCardDao
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val imported: Int, val alreadyOwned: Int)

/** Writes the collection to, and merges it back from, a JSON file the user picks. */
class BackupRepository(
    private val context: Context,
    private val dao: OwnedCardDao,
    private val settings: SettingsRepository,
) {
    /** Returns the number of cards written. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val cards = dao.getAll()
        val text = BackupCodec.encode(cards, settings.includeRegionalForms.value, System.currentTimeMillis())
        val stream = context.contentResolver.openOutputStream(uri) ?: error("Couldn't open $uri")
        stream.use { it.write(text.toByteArray()) }
        cards.size
    }

    /**
     * Adds the backup's cards to the collection. Cards that are already owned are left as they
     * are, so importing never loses anything.
     */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Couldn't open $uri")
        val bytes = stream.use { it.readBytes() }
        if (bytes.size > MAX_BYTES) throw InvalidBackupException("File is too large to be a backup")
        val backup = BackupCodec.decode(bytes.decodeToString())
        val valid = backup.cards.filter { it.dex in 1..Generation.MAX_DEX }
        val inserted = dao.insertAll(valid).count { it != -1L }
        settings.setIncludeRegionalForms(backup.includeRegionalForms)
        ImportResult(imported = inserted, alreadyOwned = valid.size - inserted)
    }

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
