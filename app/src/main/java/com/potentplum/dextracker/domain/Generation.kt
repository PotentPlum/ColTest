package com.potentplum.dextracker.domain

/** The generations covered by v1.0, with their National Dex ranges. */
enum class Generation(val number: Int, val label: String, val dexRange: IntRange) {
    GEN_1(1, "Gen 1", 1..151),
    GEN_2(2, "Gen 2", 152..251),
    GEN_3(3, "Gen 3", 252..386);

    companion object {
        val MAX_DEX: Int = entries.last().dexRange.last

        fun forDex(dex: Int): Generation =
            entries.firstOrNull { dex in it.dexRange } ?: GEN_1
    }
}

fun Int.dexLabel(): String = "#" + toString().padStart(3, '0')
