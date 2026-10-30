package com.potentplum.dextracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preferences, kept in SharedPreferences so Android backup picks them up too. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _includeRegionalForms = MutableStateFlow(prefs.getBoolean(KEY_REGIONAL, false))
    /** Whether Alolan, Galarian, Hisuian and Paldean cards count toward the base Pokémon. */
    val includeRegionalForms: StateFlow<Boolean> = _includeRegionalForms.asStateFlow()

    fun setIncludeRegionalForms(include: Boolean) {
        prefs.edit().putBoolean(KEY_REGIONAL, include).apply()
        _includeRegionalForms.value = include
    }

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_REGIONAL = "include_regional_forms"
    }
}
