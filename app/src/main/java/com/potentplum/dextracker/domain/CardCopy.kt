package com.potentplum.dextracker.domain

/**
 * A print variant of a card. The keys are the ones the TCG API uses for its per-variant
 * TCGplayer prices, which is the only place it says which variants a card was printed in.
 */
enum class CardVariant(val key: String, val label: String) {
    NORMAL("normal", "Normal"),
    HOLO("holofoil", "Holo"),
    REVERSE_HOLO("reverseHolofoil", "Reverse Holo"),
    UNLIMITED("unlimited", "Unlimited"),
    UNLIMITED_HOLO("unlimitedHolofoil", "Unlimited Holo"),
    FIRST_EDITION("1stEdition", "1st Edition"),
    FIRST_EDITION_HOLO("1stEditionHolofoil", "1st Edition Holo"),
    /** Cards logged before variants were tracked. */
    UNSPECIFIED("unspecified", "Variant not set");

    companion object {
        /** Older sets list the plain 1st Edition print under this key as well. */
        private val aliases = mapOf("1stEditionNormal" to FIRST_EDITION)

        fun fromKey(key: String): CardVariant? =
            entries.firstOrNull { it.key == key } ?: aliases[key]

        /** Parses the API's variant keys, dropping unknown ones, in display order. */
        fun parse(keys: Collection<String>): List<CardVariant> =
            keys.mapNotNull(::fromKey).filter { it != UNSPECIFIED }.distinct().sortedBy { it.ordinal }

        /** Variants a card can be logged as; a plain print when the API doesn't say. */
        fun optionsFor(available: List<CardVariant>): List<CardVariant> =
            available.ifEmpty { listOf(NORMAL) }

        /**
         * The variant most people own: the regular print rather than the reverse holo or the
         * scarcer 1st Edition.
         */
        fun defaultFor(available: List<CardVariant>): CardVariant {
            val preference = listOf(UNLIMITED_HOLO, UNLIMITED, HOLO, NORMAL, REVERSE_HOLO, FIRST_EDITION_HOLO, FIRST_EDITION)
            return preference.firstOrNull { it in available } ?: NORMAL
        }
    }
}

/** Cardmarket's grading scale, which is what European collectors use. */
enum class CardCondition(val code: String, val label: String) {
    MINT("M", "Mint"),
    NEAR_MINT("NM", "Near Mint"),
    EXCELLENT("EX", "Excellent"),
    GOOD("GD", "Good"),
    LIGHT_PLAYED("LP", "Light Played"),
    PLAYED("PL", "Played"),
    POOR("PO", "Poor");

    companion object {
        fun fromCode(code: String): CardCondition = entries.firstOrNull { it.code == code } ?: NEAR_MINT
    }
}

/** Languages Pokémon cards are printed in. */
enum class CardLanguage(val code: String, val label: String) {
    ENGLISH("EN", "English"),
    GERMAN("DE", "German"),
    FRENCH("FR", "French"),
    ITALIAN("IT", "Italian"),
    SPANISH("ES", "Spanish"),
    PORTUGUESE("PT", "Portuguese"),
    DUTCH("NL", "Dutch"),
    POLISH("PL", "Polish"),
    JAPANESE("JA", "Japanese"),
    KOREAN("KO", "Korean"),
    CHINESE("ZH", "Chinese");

    companion object {
        fun fromCode(code: String): CardLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/** Cardmarket trend prices for a card, in euros. */
data class CardPrices(
    val trend: Double?,
    val reverseHoloTrend: Double?,
) {
    /**
     * Value of one copy. Cardmarket prices reverse holos separately but doesn't split out
     * 1st Edition, so every other variant uses the card's main trend price.
     */
    fun unitValue(variant: CardVariant): Double? {
        val reverse = reverseHoloTrend?.takeIf { it > 0 }
        val main = trend?.takeIf { it > 0 }
        return if (variant == CardVariant.REVERSE_HOLO) reverse ?: main else main
    }

    companion object {
        val NONE = CardPrices(null, null)
    }
}
