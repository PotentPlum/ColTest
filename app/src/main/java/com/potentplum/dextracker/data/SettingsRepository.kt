package com.potentplum.dextracker.data

import android.content.Context
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preferences, kept in SharedPreferences so Android backup picks them up too. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _includeRegionalForms = MutableStateFlow(prefs.getBoolean(KEY_REGIONAL, false))
    /** Whether Alolan, Galarian, Hisuian and Paldean cards count toward the base Pokémon. */
    val includeRegionalForms: StateFlow<Boolean> = _includeRegionalForms.asStateFlow()

    private val _defaultCondition = MutableStateFlow(CardCondition.fromCode(prefs.getString(KEY_CONDITION, "").orEmpty()))
    /** Condition new copies get; follows whatever the user picked last. */
    val defaultCondition: StateFlow<CardCondition> = _defaultCondition.asStateFlow()

    private val _defaultLanguage = MutableStateFlow(CardLanguage.fromCode(prefs.getString(KEY_LANGUAGE, "").orEmpty()))
    /** Language new copies get; follows whatever the user picked last. */
    val defaultLanguage: StateFlow<CardLanguage> = _defaultLanguage.asStateFlow()

    fun setIncludeRegionalForms(include: Boolean) {
        prefs.edit().putBoolean(KEY_REGIONAL, include).apply()
        _includeRegionalForms.value = include
    }

    fun setDefaultCondition(condition: CardCondition) {
        prefs.edit().putString(KEY_CONDITION, condition.code).apply()
        _defaultCondition.value = condition
    }

    fun setDefaultLanguage(language: CardLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _defaultLanguage.value = language
    }

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_REGIONAL = "include_regional_forms"
        const val KEY_CONDITION = "default_condition"
        const val KEY_LANGUAGE = "default_language"
    }
}
