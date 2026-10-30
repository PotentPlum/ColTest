package com.potentplum.dextracker.ui.components

import java.text.NumberFormat
import java.util.Currency

private val euroFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance("EUR")
}

/** Cardmarket prices are in euros, shown in the user's own number format. */
fun euro(amount: Double): String = euroFormat.format(amount)
