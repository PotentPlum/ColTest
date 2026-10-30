package com.potentplum.dextracker.domain

/** One line of owned copies with its value per copy, if Cardmarket has a price for it. */
data class ValuedCopy(
    val cardId: String,
    val dex: Int,
    val name: String,
    val setName: String,
    val number: String,
    val imageSmall: String,
    val variant: CardVariant,
    val condition: CardCondition,
    val language: CardLanguage,
    val quantity: Int,
    val unitValue: Double?,
) {
    val totalValue: Double get() = (unitValue ?: 0.0) * quantity
}

data class CollectionValuation(
    /** Worth of every copy with a known price, in euros. */
    val total: Double,
    val pricedCopies: Int,
    val totalCopies: Int,
    /** Copies worth more than [Valuation.NOTABLE_OVER_EUR] each, most valuable first. */
    val notable: List<ValuedCopy>,
)

object Valuation {
    /** Only cards worth more than this per copy are listed; everything counts toward the total. */
    const val NOTABLE_OVER_EUR = 3.0

    fun of(copies: List<ValuedCopy>): CollectionValuation = CollectionValuation(
        total = copies.sumOf { it.totalValue },
        pricedCopies = copies.filter { it.unitValue != null }.sumOf { it.quantity },
        totalCopies = copies.sumOf { it.quantity },
        notable = copies
            .filter { (it.unitValue ?: 0.0) > NOTABLE_OVER_EUR }
            .sortedWith(compareByDescending<ValuedCopy> { it.unitValue }.thenBy { it.dex }),
    )
}
