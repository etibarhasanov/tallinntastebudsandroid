package ee.tallinntastebuds.model

import kotlinx.serialization.Serializable

/**
 * `data/radio.json`: one default station plus a per-language override, so the
 * station follows whichever language the reader picked.
 */
@Serializable
data class RadioFeed(
    val default: RadioStation? = null,
    val byLanguage: Map<String, RadioStation>? = null,
) {
    fun stationFor(lang: String): RadioStation? = byLanguage?.get(lang) ?: default
}

@Serializable
data class RadioStation(
    val name: String,
    val url: String,
)
