package com.castivio.domain

/**
 * A language a viewer can prefer for a film's sound or for its subtitles (Settings → Player), and how a track is
 * recognised as being in it.
 *
 * A track says its language in whatever spelling its container and its engine give it: Media3 hands over a two-letter
 * code (`ar`), sometimes with a region (`ar-SA`); a Matroska file read by LibVLC can carry the three-letter one, in either
 * of the two forms ISO 639-2 has (`fre` and `fra`); a provider's label can be the language's English name or its own
 * ("Arabic", "العربية"), or nothing at all. [matches] accepts each of these and nothing that merely contains them.
 *
 * Each language is named in itself ([endonym]) in the list the viewer picks from, so the list reads the same in every
 * one of the application's languages and needs no translation.
 */
enum class TrackLanguage(
    /** The two-letter code the choice is kept under. */
    val code: String,
    /** The language's name in itself: what the list shows. */
    val endonym: String,
    /** The three-letter codes and the English name, besides [code] and [endonym]. */
    private val also: List<String>,
) {
    ARABIC("ar", "العربية", listOf("ara", "arabic")),
    ENGLISH("en", "English", listOf("eng", "english")),
    FRENCH("fr", "Français", listOf("fra", "fre", "french")),
    SPANISH("es", "Español", listOf("spa", "spanish")),
    GERMAN("de", "Deutsch", listOf("deu", "ger", "german")),
    TURKISH("tr", "Türkçe", listOf("tur", "turkish")),
    ITALIAN("it", "Italiano", listOf("ita", "italian")),
    PORTUGUESE("pt", "Português", listOf("por", "portuguese")),
    DUTCH("nl", "Nederlands", listOf("nld", "dut", "dutch")),
    RUSSIAN("ru", "Русский", listOf("rus", "russian")),
    PERSIAN("fa", "فارسی", listOf("fas", "per", "persian", "farsi")),
    URDU("ur", "اردو", listOf("urd", "urdu")),
    HINDI("hi", "हिन्दी", listOf("hin", "hindi")),
    ;

    private val spellings: Set<String> = (listOf(code, endonym) + also).map { it.lowercase() }.toSet()

    /**
     * Whether a track whose language is [language] and whose label is [label] is in this language.
     *
     * The language first, taken up to a region or a script (`ar-SA`, `pt_BR`, `zh-Hant`); the label only when the track
     * gave no language, and then only as a whole word -- "Arabic", "Arabic 5.1", "العربية" -- so that a label such as
     * "Original" or "Track 2" is never read as a language.
     */
    fun matches(language: String?, label: String?): Boolean {
        val tag = language?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
        if (!tag.isNullOrEmpty() && tag != UNDETERMINED) return tag in spellings
        val words = label?.lowercase()?.split(WORD_BREAK)?.filter { it.isNotEmpty() }.orEmpty()
        return words.any { it in spellings }
    }

    companion object {
        /** The language kept under [code], or null for none -- the original sound, or no subtitles. */
        fun of(code: String?): TrackLanguage? = code?.let { wanted -> entries.firstOrNull { it.code == wanted } }

        /** ISO 639-2's code for "undetermined", which some files write in place of nothing. */
        private const val UNDETERMINED = "und"

        private val WORD_BREAK = Regex("[^\\p{L}]+")
    }
}

/**
 * The languages the viewer prefers for sound and for subtitles, kept between launches. Null is no preference: the sound the
 * film starts with, and subtitles left as the film has them.
 */
interface TrackLanguagePreferences {
    fun audio(): TrackLanguage?
    fun subtitle(): TrackLanguage?
    fun setAudio(language: TrackLanguage?)
    fun setSubtitle(language: TrackLanguage?)

    /** None kept: what a build or a test with nowhere to keep them has. */
    object None : TrackLanguagePreferences {
        override fun audio(): TrackLanguage? = null
        override fun subtitle(): TrackLanguage? = null
        override fun setAudio(language: TrackLanguage?) = Unit
        override fun setSubtitle(language: TrackLanguage?) = Unit
    }
}
