package com.castivio.feature.home

import com.castivio.domain.MediaGroup
import com.castivio.domain.SortOrder
import java.text.Collator
import java.util.Locale

/**
 * The rail's bouquets in the order the viewer asked the board for: the provider's own -- or the viewer's, where they arranged
 * them -- as they are given, or by name.
 *
 * **Names are compared as a reader orders them, not as code points.** Case and accents do not decide, so "fr| france hd"
 * stands with "FR| FRANCE HD", and a digit comes before a letter. Two names that compare equal stay in the order they were
 * given, which is the provider's, so one list always comes out the same. `Locale.ROOT`, so a device's language does not change
 * which bouquet comes first.
 *
 * Only the two orders by name move anything: the provider's order is the list as it is, and a bouquet has no date for
 * "recently added" to go by.
 */
internal fun List<MediaGroup>.inOrder(order: SortOrder): List<MediaGroup> = when (order) {
    SortOrder.NAME_ASC -> sortedWith(BY_NAME)
    SortOrder.NAME_DESC -> sortedWith(BY_NAME.reversed())
    SortOrder.PROVIDER, SortOrder.RECENTLY_ADDED -> this
}

/**
 * The channels' bouquets have two orders to choose between, and a press goes to the other one: the provider's, or by name.
 * Whatever else it was -- the films' board has four -- it goes to the provider's, which is where a bouquet is not moved.
 */
internal fun SortOrder.otherBouquetOrder(): SortOrder =
    if (this == SortOrder.PROVIDER) SortOrder.NAME_ASC else SortOrder.PROVIDER

/** Compared with the collator's primary strength -- the letters and nothing laid over them -- and one for all lists. */
private val BY_NAME: Comparator<MediaGroup> = Collator.getInstance(Locale.ROOT)
    .apply { strength = Collator.PRIMARY }
    .let { collator -> Comparator { a, b -> collator.compare(a.name, b.name) } }
