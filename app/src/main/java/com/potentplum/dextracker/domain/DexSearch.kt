package com.potentplum.dextracker.domain

import java.text.Normalizer

/**
 * Matches the grid search box against a Pokémon. A number ("25", "#025") jumps to that exact
 * Dex entry; anything else is a name search that ignores case, accents and punctuation, so
 * "mr mime", "farfetchd" and "flabebe" all find their Pokémon.
 */
object DexSearch {
    fun matches(query: String, dex: Int, name: String): Boolean {
        val trimmed = query.trim().removePrefix("#").trim()
        if (trimmed.isEmpty()) return true
        trimmed.toIntOrNull()?.let { return it == dex }
        val needle = trimmed.simplified()
        return needle.isNotEmpty() && needle in name.simplified()
    }

    private fun String.simplified(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '♀' || it == '♂' }
}
