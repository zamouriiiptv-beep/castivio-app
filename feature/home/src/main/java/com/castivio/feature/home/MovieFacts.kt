package com.castivio.feature.home

import androidx.compose.ui.unit.Dp
import com.castivio.core.design.components.ltrIsolate
import com.castivio.domain.Movie
import com.castivio.domain.MovieDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What the films screen says about a film, and what it leaves unsaid.
 */
internal fun movieDuration(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

internal fun movieFacts(movie: Movie): List<String> =
    listOfNotNull(movie.year?.takeIf { it > 0 }?.toString(), movieDuration(movie.durationMinutes))

internal fun Movie.withDetails(details: MovieDetails?): Movie {
    if (details == null) return this
    return copy(
        year = year ?: details.releaseYear,
        durationMinutes = durationMinutes ?: details.durationSeconds?.let { it / SECONDS_PER_MINUTE },
        genres = genres.ifEmpty { details.genres },
    )
}

/**
 * Remove a trailing cast-name fragment only when it follows a release year and exactly matches
 * the beginning of a known cast member's name. Unrecognized suffixes are preserved.
 */
internal fun movieDisplayTitle(title: String, cast: List<String>): String {
    val year = Regex("""\\s*\\(\\d{4}\\)(.*)$""").find(title) ?: return title
    val suffix = year.groupValues[1].trim().trim('-', '–', '—', ':', '|').trim()
    if (suffix.isEmpty()) return title
    val suffixWords = suffix.split(Regex("""\\s+"""))
    val isCastFragment = cast.any { person ->
        val personWords = person.trim().split(Regex("""\\s+""")).filter { it.isNotEmpty() }
        suffixWords.size <= personWords.size &&
            suffixWords.indices.all { suffixWords[it].equals(personWords[it], ignoreCase = true) }
    }
    return if (isCastFragment) title.substring(0, year.range.last + 1).trimEnd() else title
}

/** Xtream's director field is a comma-separated list; display its leading director, not the whole crew string. */
internal fun primaryDirector(value: String?): String? =
    value.orEmpty().split(',').firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

internal fun ratingText(rating: Double): String = String.format(Locale.ROOT, "%.1f", rating)

/** The names stay in provider order; isolate the complete Latin run, including its separators, in RTL layouts. */
// Keep the separator attached to the preceding name. A normal space before the dot
// lets Compose wrap it onto the next line, where it looks like punctuation at line start.
private const val CREDIT_SEPARATOR = "\u00A0· "
private const val NO_BREAK_SPACE = '\u00A0'

internal fun creditNames(names: List<String>, rtl: Boolean): String {
    val joined = names.joinToString(CREDIT_SEPARATOR) { it.replace(' ', NO_BREAK_SPACE) }
    return if (rtl && joined.isNotEmpty()) ltrIsolate(joined) else names.joinToString(", ")
}

internal fun creditLines(fitted: Boolean, whenFitted: Int): Int = if (fitted) whenFitted else Int.MAX_VALUE

internal fun posterRating(movie: Movie): String? =
    movie.rating?.takeIf { it.isFinite() && it > 0 }?.let(::ratingText)

internal fun addedDateText(epochSeconds: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(epochSeconds * MILLIS_PER_SECOND))

internal sealed interface MovieList {
    data object All : MovieList
    data object Favorites : MovieList
    data object Recent : MovieList
    data class Category(val id: String) : MovieList
}

internal fun BrowseState.movieList(): MovieList {
    val group = selectedGroup
    return when {
        favorites -> MovieList.Favorites
        recent -> MovieList.Recent
        group != null -> MovieList.Category(group)
        else -> MovieList.All
    }
}

internal fun BrowseState.headingCount(): Int? =
    if (search.isNotEmpty() || favorites || recent) null else total

internal fun posterColumns(available: Dp, gutter: Dp, minimum: Dp): Int =
    ((available + gutter) / (minimum + gutter)).toInt().coerceAtLeast(MIN_POSTER_COLUMNS)

internal fun posterHeightFor(available: Dp, rowExtra: Dp, rowGap: Dp, peek: Float, floor: Dp): Dp =
    ((available - (rowExtra + rowGap) * 2) / (2f + peek)).coerceAtLeast(floor)

internal fun posterCardWidth(available: Dp, gutter: Dp, columns: Int): Dp =
    (available - gutter * (columns - 1)) / columns

private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val MILLIS_PER_SECOND = 1_000L
private const val MIN_POSTER_COLUMNS = 2
