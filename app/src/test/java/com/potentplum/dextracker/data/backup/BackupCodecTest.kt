package com.potentplum.dextracker.data.backup

import com.potentplum.dextracker.data.local.CardEntity
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.local.OwnedCopyEntity
import com.potentplum.dextracker.data.local.WantedCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val pikachu = CardEntity(
        cardId = "base1-58",
        dex = 25,
        name = "Pikachu",
        setId = "base1",
        setName = "Base",
        setSeries = "Base",
        releaseDate = "1999/01/09",
        number = "58",
        rarity = "Common",
        imageSmall = "https://images.pokemontcg.io/base1/58.png",
        imageLarge = "https://images.pokemontcg.io/base1/58_hires.png",
        variants = "1stEdition,unlimited",
        priceTrend = 14.44,
        priceReverseTrend = null,
        pricesUpdatedAt = 5L,
    )

    private val copies = listOf(
        OwnedCopyEntity(1, "base1-58", "1stEdition", "EX", "DE", 1, 100),
        OwnedCopyEntity(2, "base1-58", "unlimited", "NM", "EN", 3, 200),
    )

    @Test
    fun `round trips cards, copies and settings`() {
        val text = BackupCodec.encode(listOf(CardWithCopies(pikachu, copies)), includeRegionalForms = true, exportedAt = 1L)
        val decoded = BackupCodec.decode(text)
        val card = decoded.cards.single()
        assertTrue(decoded.includeRegionalForms)
        // Prices aren't part of a backup; they're refreshed from Cardmarket.
        assertEquals(pikachu.copy(priceTrend = null, pricesUpdatedAt = null), card.card)
        // Row ids are local to a database, so restored copies get new ones.
        assertEquals(copies.map { it.copy(id = 0) }, card.copies)
    }

    @Test
    fun `reads version 1 backups as one unspecified copy per card`() {
        val text = """
            {"format":"pokedex-tracker-backup","version":1,"exportedAt":1,
             "cards":[{"cardId":"a","dex":1,"name":"Bulbasaur","setId":"s","setName":"S","setSeries":"S",
                       "releaseDate":"1999/01/09","number":"44","imageSmall":"x","imageLarge":"y","addedAt":5}]}
        """.trimIndent()
        val card = BackupCodec.decode(text).cards.single()
        assertEquals("a", card.card.cardId)
        assertNull(card.card.rarity)
        assertEquals(listOf(OwnedCopyEntity(0, "a", "unspecified", "NM", "EN", 1, 5)), card.copies)
    }

    @Test
    fun `ignores unknown fields and drops empty copies`() {
        val text = """
            {"format":"pokedex-tracker-backup","version":2,"exportedAt":1,"extra":true,
             "cards":[{"cardId":"a","dex":1,"name":"Bulbasaur","setId":"s","setName":"S","setSeries":"S",
                       "releaseDate":"1999/01/09","number":"44","imageSmall":"x","imageLarge":"y","grade":10,
                       "copies":[{"variant":"holofoil","condition":"NM","language":"EN","quantity":0,"addedAt":1},
                                 {"variant":"normal","condition":"GD","language":"FR","quantity":2,"addedAt":1}]}]}
        """.trimIndent()
        val copy = BackupCodec.decode(text).cards.single().copies.single()
        assertEquals("normal", copy.variant)
        assertEquals(2, copy.quantity)
    }

    @Test
    fun `round trips the wanted list and trainer cards`() {
        val trainer = pikachu.copy(cardId = "me55-129", dex = 0, name = "Ultra Ball", variants = "")
        val wanted = WantedCardEntity(
            cardId = "me55-132",
            dex = 144,
            name = "Articuno",
            setId = "me55",
            setName = "30th Celebration",
            setSeries = "Mega Evolution",
            releaseDate = "2026/09/16",
            number = "132",
            rarity = "Illustration Rare",
            imageSmall = "s",
            imageLarge = "l",
            variants = "holofoil",
            priceTrend = 14.8,
            priceReverseTrend = null,
            pricesUpdatedAt = 9L,
            addedAt = 42L,
        )
        val text = BackupCodec.encode(
            listOf(CardWithCopies(trainer, copies.map { it.copy(cardId = trainer.cardId) })),
            includeRegionalForms = false,
            exportedAt = 1L,
            wanted = listOf(wanted),
        )
        val decoded = BackupCodec.decode(text)
        assertEquals(0, decoded.cards.single().card.dex)
        // Like owned cards, prices aren't backed up.
        assertEquals(listOf(wanted.copy(priceTrend = null, pricesUpdatedAt = null)), decoded.wanted)
    }

    @Test
    fun `reads backups from before the wanted list as an empty list`() {
        val text = """{"format":"pokedex-tracker-backup","version":2,"exportedAt":1,"cards":[]}"""
        assertEquals(emptyList<WantedCardEntity>(), BackupCodec.decode(text).wanted)
    }

    @Test(expected = InvalidBackupException::class)
    fun `rejects other json files`() {
        BackupCodec.decode("""{"hello":"world"}""")
    }

    @Test(expected = InvalidBackupException::class)
    fun `rejects backups from a newer format`() {
        BackupCodec.decode("""{"format":"pokedex-tracker-backup","version":99,"exportedAt":1,"cards":[]}""")
    }

    @Test(expected = InvalidBackupException::class)
    fun `rejects non json`() {
        BackupCodec.decode("not json at all")
    }
}
