package com.potentplum.dextracker.domain

/** Every main-series generation, with its National Dex range. */
enum class Generation(val number: Int, val label: String, val dexRange: IntRange) {
    GEN_1(1, "Gen 1", 1..151),
    GEN_2(2, "Gen 2", 152..251),
    GEN_3(3, "Gen 3", 252..386),
    GEN_4(4, "Gen 4", 387..493),
    GEN_5(5, "Gen 5", 494..649),
    GEN_6(6, "Gen 6", 650..721),
    GEN_7(7, "Gen 7", 722..809),
    GEN_8(8, "Gen 8", 810..905),
    GEN_9(9, "Gen 9", 906..1025);

    val size: Int get() = dexRange.count()

    companion object {
        val MAX_DEX: Int = entries.last().dexRange.last

        fun forDex(dex: Int): Generation =
            entries.firstOrNull { dex in it.dexRange } ?: GEN_1
    }
}

fun Int.dexLabel(): String = "#" + toString().padStart(3, '0')
