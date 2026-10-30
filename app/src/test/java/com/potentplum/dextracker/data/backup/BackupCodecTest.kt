package com.potentplum.dextracker.data.backup

import com.potentplum.dextracker.data.local.OwnedCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val pikachu = OwnedCardEntity(
        cardId = "base1-58",
        dex = 25,
        name = "Pikachu",
        setId = "base1",
        setName = "Base",
        setSeries = "Base",
        releaseDate = "1999/01/09",
        number = "58",
        rarity = null,
        imageSmall = "https://images.pokemontcg.io/base1/58.png",
        imageLarge = "https://images.pokemontcg.io/base1/58_hires.png",
        addedAt = 1_700_000_000_000,
    )

    @Test
    fun `round trips cards and settings`() {
        val text = BackupCodec.encode(listOf(pikachu), includeRegionalForms = true, exportedAt = 1L)
        val decoded = BackupCodec.decode(text)
        assertEquals(listOf(pikachu), decoded.cards)
        assertTrue(decoded.includeRegionalForms)
    }

    @Test
    fun `ignores fields added by newer minor changes`() {
        val text = """
            {"format":"pokedex-tracker-backup","version":1,"exportedAt":1,"extra":true,
             "cards":[{"cardId":"a","dex":1,"name":"Bulbasaur","setId":"s","setName":"S","setSeries":"S",
                       "releaseDate":"1999/01/09","number":"44","imageSmall":"x","imageLarge":"y",
                       "addedAt":5,"condition":"NM"}]}
        """.trimIndent()
        val decoded = BackupCodec.decode(text)
        assertEquals("a", decoded.cards.single().cardId)
        assertEquals(null, decoded.cards.single().rarity)
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
