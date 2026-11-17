package com.castivio.feature.home

import com.castivio.domain.MediaGroup
import com.castivio.domain.MediaKind
import com.castivio.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The rail's bouquets by name, as a reader orders them: case and accents do not decide, a digit comes before a letter,
 * names that compare equal stay as the provider gave them, and the provider's order is the list untouched.
 *
 * The names are a provider's own -- a country's mark and a name, "FR| FRANCE HD" -- so what is checked is what the
 * viewer sees: bouquets of one mark stand together, and inside the mark the names run in order.
 */
class BouquetOrderTest {

    private fun group(id: String, name: String) = MediaGroup(id = id, name = name, kind = MediaKind.LIVE, itemCount = 1)

    private fun ids(groups: List<MediaGroup>) = groups.map { it.id }

    /** As the provider wrote them -- the order the rail has now, which is not the alphabet's. */
    private val france = listOf(
        group("vip", "FR| FRANCE VIP RAW"),
        group("hd", "FR| FRANCE HD"),
        group("k4", "FR| FRANCE 4K"),
        group("sport", "FR| FRANCE SPORT VIP RAW"),
        group("lq", "FR| FRANCE LQ"),
        group("fhd", "FR| FRANCE FHD"),
    )

    @Test
    fun `the provider's order is the list as it was given`() {
        assertSame(france, france.inOrder(SortOrder.PROVIDER))
    }

    @Test
    fun `recently added is no order for a bouquet, which has no date, and leaves the list as it was`() {
        assertSame(france, france.inOrder(SortOrder.RECENTLY_ADDED))
    }

    @Test
    fun `by name the bouquets run in order, a digit before a letter`() {
        assertEquals(listOf("k4", "fhd", "hd", "lq", "sport", "vip"), ids(france.inOrder(SortOrder.NAME_ASC)))
    }

    @Test
    fun `from the end of the alphabet they run the other way`() {
        assertEquals(listOf("vip", "sport", "lq", "hd", "fhd", "k4"), ids(france.inOrder(SortOrder.NAME_DESC)))
    }

    @Test
    fun `case does not decide`() {
        val mixed = listOf(group("c", "charlie"), group("a", "ALPHA"), group("b", "Bravo"))

        assertEquals(listOf("a", "b", "c"), ids(mixed.inOrder(SortOrder.NAME_ASC)))
    }

    @Test
    fun `bouquets of one mark stand together, in order inside it`() {
        val mixed = listOf(
            group("vid", "|NL| VIDEOLAND"),
            group("npo", "|NL| NPO PLUS"),
            group("fr", "FR| FRANCE HD"),
            group("net", "|NL| NETFLIX"),
        )

        val sorted = ids(mixed.inOrder(SortOrder.NAME_ASC))

        // Wherever the collator puts a mark's own punctuation among the letters, the three of one mark are one run.
        val nl = sorted.indices.filter { sorted[it] in setOf("net", "npo", "vid") }
        assertEquals(listOf(nl.first(), nl.first() + 1, nl.first() + 2), nl)
        assertEquals(listOf("net", "npo", "vid"), nl.map { sorted[it] })
    }

    @Test
    fun `names that compare equal stay as the provider gave them, in either direction`() {
        val twins = listOf(group("one", "Sports"), group("two", "SPORTS"), group("zed", "Kids"))

        assertEquals(listOf("zed", "one", "two"), ids(twins.inOrder(SortOrder.NAME_ASC)))
        assertEquals(listOf("one", "two", "zed"), ids(twins.inOrder(SortOrder.NAME_DESC)))
    }

    @Test
    fun `the list that was given is not changed`() {
        val before = ids(france)

        france.inOrder(SortOrder.NAME_ASC)

        assertEquals(before, ids(france))
    }

    @Test
    fun `no bouquets is no list`() {
        assertEquals(emptyList<MediaGroup>(), emptyList<MediaGroup>().inOrder(SortOrder.NAME_ASC))
    }

    // ------------------------------------------------------------------------------------ the control

    @Test
    fun `a press on the order goes from the provider's to by name`() {
        assertEquals(SortOrder.NAME_ASC, SortOrder.PROVIDER.otherBouquetOrder())
    }

    @Test
    fun `a press on any other order goes back to the provider's`() {
        assertEquals(SortOrder.PROVIDER, SortOrder.NAME_ASC.otherBouquetOrder())
        assertEquals(SortOrder.PROVIDER, SortOrder.NAME_DESC.otherBouquetOrder())
        assertEquals(SortOrder.PROVIDER, SortOrder.RECENTLY_ADDED.otherBouquetOrder())
    }
}
