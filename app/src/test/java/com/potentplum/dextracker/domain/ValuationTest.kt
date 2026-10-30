package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ValuationTest {
    private fun copy(id: String, value: Double?, quantity: Int = 1, dex: Int = 25) = ValuedCopy(
        cardId = id, dex = dex, name = "Pikachu", setName = "Base", number = "1", imageSmall = "",
        variant = CardVariant.HOLO, condition = CardCondition.NEAR_MINT, language = CardLanguage.ENGLISH,
        quantity = quantity, unitValue = value,
    )

    @Test
    fun `total counts every priced copy`() {
        val valuation = Valuation.of(listOf(copy("a", 10.0, quantity = 2), copy("b", 0.5), copy("c", null)))
        assertEquals(20.5, valuation.total, 0.0001)
        assertEquals(3, valuation.pricedCopies)
        assertEquals(4, valuation.totalCopies)
    }

    @Test
    fun `lists only copies worth more than three euros, most valuable first`() {
        val valuation = Valuation.of(
            listOf(copy("cheap", 2.99), copy("exactly", 3.0), copy("mid", 3.01), copy("top", 120.0), copy("none", null)),
        )
        assertEquals(listOf("top", "mid"), valuation.notable.map { it.cardId })
    }
}
