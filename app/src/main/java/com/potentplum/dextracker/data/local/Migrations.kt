package com.potentplum.dextracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 kept one row per owned card. v2 splits that into `card` (what the card is, plus prices)
 * and `owned_copy` (variant, condition, language and quantity). Every v1 card becomes one
 * near-mint English copy with its variant not set, so nothing in a collection is lost.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2_SQL.forEach(db::execSQL)
    }
}

/** Kept separate from [MIGRATION_1_2] so tests can run it against a plain SQLite database. */
internal val MIGRATION_1_2_SQL = listOf(
    "CREATE TABLE IF NOT EXISTS `card` (`cardId` TEXT NOT NULL, `dex` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
        "`setId` TEXT NOT NULL, `setName` TEXT NOT NULL, `setSeries` TEXT NOT NULL, `releaseDate` TEXT NOT NULL, " +
        "`number` TEXT NOT NULL, `rarity` TEXT, `imageSmall` TEXT NOT NULL, `imageLarge` TEXT NOT NULL, " +
        "`variants` TEXT NOT NULL, `priceTrend` REAL, `priceReverseTrend` REAL, `pricesUpdatedAt` INTEGER, " +
        "PRIMARY KEY(`cardId`))",
    "CREATE INDEX IF NOT EXISTS `index_card_dex` ON `card` (`dex`)",
    "CREATE TABLE IF NOT EXISTS `owned_copy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`cardId` TEXT NOT NULL, `variant` TEXT NOT NULL, `condition` TEXT NOT NULL, `language` TEXT NOT NULL, " +
        "`quantity` INTEGER NOT NULL, `added_at` INTEGER NOT NULL, " +
        "FOREIGN KEY(`cardId`) REFERENCES `card`(`cardId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_owned_copy_cardId_variant_condition_language` " +
        "ON `owned_copy` (`cardId`, `variant`, `condition`, `language`)",
    "INSERT INTO `card` (cardId, dex, name, setId, setName, setSeries, releaseDate, number, rarity, " +
        "imageSmall, imageLarge, variants, priceTrend, priceReverseTrend, pricesUpdatedAt) " +
        "SELECT cardId, dex, name, setId, setName, setSeries, releaseDate, number, rarity, " +
        "imageSmall, imageLarge, '', NULL, NULL, NULL FROM `owned_card`",
    "INSERT INTO `owned_copy` (cardId, variant, condition, language, quantity, added_at) " +
        "SELECT cardId, 'unspecified', 'NM', 'EN', 1, added_at FROM `owned_card`",
    "DROP TABLE `owned_card`",
)
