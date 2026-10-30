package com.potentplum.dextracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFormatterTest {
    @Test
    fun `capitalises plain slugs`() {
        assertEquals("Pikachu", NameFormatter.fromSlug("pikachu"))
    }

    @Test
    fun `handles special gen 1 to 3 names`() {
        assertEquals("Nidoran♀", NameFormatter.fromSlug("nidoran-f"))
        assertEquals("Mr. Mime", NameFormatter.fromSlug("mr-mime"))
        assertEquals("Farfetch'd", NameFormatter.fromSlug("farfetchd"))
        assertEquals("Ho-Oh", NameFormatter.fromSlug("ho-oh"))
    }

    @Test
    fun `handles special gen 4 to 9 names`() {
        assertEquals("Mime Jr.", NameFormatter.fromSlug("mime-jr"))
        assertEquals("Porygon-Z", NameFormatter.fromSlug("porygon-z"))
        assertEquals("Flabébé", NameFormatter.fromSlug("flabebe"))
        assertEquals("Type: Null", NameFormatter.fromSlug("type-null"))
        assertEquals("Kommo-o", NameFormatter.fromSlug("kommo-o"))
        assertEquals("Sirfetch'd", NameFormatter.fromSlug("sirfetchd"))
        assertEquals("Chi-Yu", NameFormatter.fromSlug("chi-yu"))
        assertEquals("Tapu Koko", NameFormatter.fromSlug("tapu-koko"))
        assertEquals("Iron Valiant", NameFormatter.fromSlug("iron-valiant"))
    }
}
