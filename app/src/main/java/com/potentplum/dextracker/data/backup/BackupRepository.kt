package com.potentplum.dextracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.local.AppDatabase
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val imported: Int, val alreadyOwned: Int)

/** Writes the collection to, and merges it back from, a JSON file the user picks. */
class BackupRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val dao = database.collectionDao()

    /** Returns the number of copies written. */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val cards = dao.getAll()
        val text = BackupCodec.encode(cards, settings.includeRegionalForms.value, System.currentTimeMillis())
        val stream = context.contentResolver.openOutputStream(uri) ?: error("Couldn't open $uri")
        stream.use { it.write(text.toByteArray()) }
        cards.sumOf { card -> card.copies.sumOf { it.quantity } }
    }

    /**
     * Adds the backup's copies to the collection. Copies that are already owned (same card,
     * variant, condition and language) are left as they are, so importing never loses anything.
     * Counts are in copies.
     */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Couldn't open $uri")
        val bytes = stream.use { it.readBytes() }
        if (bytes.size > MAX_BYTES) throw InvalidBackupException("File is too large to be a backup")
        val backup = BackupCodec.decode(bytes.decodeToString())
        val valid = backup.cards.filter { it.card.dex in 1..Generation.MAX_DEX && it.copies.isNotEmpty() }
        var imported = 0
        var skipped = 0
        database.withTransaction {
            valid.forEach { entry ->
                dao.insertCardIfAbsent(entry.card)
                dao.insertCopiesIfAbsent(entry.copies).forEachIndexed { i, rowId ->
                    if (rowId == -1L) skipped += entry.copies[i].quantity else imported += entry.copies[i].quantity
                }
            }
        }
        settings.setIncludeRegionalForms(backup.includeRegionalForms)
        ImportResult(imported = imported, alreadyOwned = skipped)
    }

    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
