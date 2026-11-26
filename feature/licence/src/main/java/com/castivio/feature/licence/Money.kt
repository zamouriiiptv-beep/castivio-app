package com.castivio.feature.licence

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * A price, in the one shape Castivio draws it: `$5.99`, everywhere.
 *
 * ## Why this is not a string resource
 *
 * A formatted price in `strings.xml` would be 39 files to change every time a price
 * does, and 39 chances to change 38 of them. `PlanOffer` carries [minor] as an integer
 * in the currency's minor unit and [currency] as ISO 4217, and the formatting happens
 * here. No string resource on this screen contains a price or a currency symbol, and
 * `LicencePricingTest` fails the build if one ever does.
 *
 * ## Why one shape rather than each language's
 *
 * The portal shows `$5.99` in all nine of its languages, and the screen a user is sent
 * from must show the price they will be asked for, in the same shape (owner,
 * 2026-11-26). It is the dates' answer too: `CastivioDates` draws one shape on every
 * screen. So the amount is laid out by US conventions — symbol first, a point, Western
 * digits — whatever the interface's language. [locale] stays in the signature so the
 * callers keep handing the interface's locale in, which is what lets a test assert the
 * shape does not move with it.
 *
 * A whole amount has no fraction (`€6`, not `€6.00`); one with minor units keeps both
 * digits. An unknown currency is formatted as a plain number with the code beside it
 * rather than thrown: a licence screen that crashes because a server sent a currency
 * this build has never heard of is worse than one that says `6 XTS`.
 */
@Suppress("UNUSED_PARAMETER")
internal fun formatPrice(minor: Long, currency: String, locale: Locale): String {
    val format = NumberFormat.getCurrencyInstance(PRICE_SHAPE)
    val units = minor.toDouble() / MINOR_UNITS
    val digits = if (minor % MINOR_UNITS == 0L) 0 else 2
    return runCatching {
        format.currency = Currency.getInstance(currency)
        format.maximumFractionDigits = digits
        format.minimumFractionDigits = digits
        format.format(units)
    }.getOrElse {
        val plain = NumberFormat.getNumberInstance(PRICE_SHAPE).apply {
            maximumFractionDigits = digits
            minimumFractionDigits = digits
        }
        "${plain.format(units)} $currency"
    }
}

/** The conventions every price is drawn in: `$5.99`. */
private val PRICE_SHAPE: Locale = Locale.US

/** Every currency Castivio prices in has two. */
private const val MINOR_UNITS = 100L
