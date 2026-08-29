package ee.tallinntastebuds.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.text.Normalizer

/**
 * One approved place, decoded straight from the website's `data/restaurants.json`.
 *
 * The field names and their meanings are fixed by `data/schema.json` in the
 * website repo. Anything optional there is optional here, and an empty string
 * means the same thing as a missing key — the site treats them identically, so
 * the properties below fold them together.
 */
@Serializable
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    /** Cost band 1..4, rendered as euro signs. Not a rating: the site has no ratings. */
    val price: Int,
    /** Type ids that exist in `taxonomy.json`. */
    val types: List<String> = emptyList(),
    /** The write-up, keyed by language code. The only per-place translated field. */
    val blurb: Map<String, String> = emptyMap(),
    val mustOrder: List<String> = emptyList(),
    /** Photo filenames inside `photos/<id>/`. */
    val photos: List<String> = emptyList(),
    val closed: Boolean = false,
    @SerialName("reel") private val reelRaw: String? = null,
    @SerialName("website") private val websiteRaw: String? = null,
    @SerialName("phone") private val phoneRaw: String? = null,
    @SerialName("added") private val addedRaw: String? = null,
    @SerialName("visited") private val visitedRaw: String? = null,
) {
    /** Instagram or TikTok permalink, or null when there is not one yet. */
    val reel: String? get() = reelRaw.nonEmpty()
    val website: String? get() = websiteRaw.nonEmpty()
    val phone: String? get() = phoneRaw.nonEmpty()

    /** `YYYY-MM-DD`, the day the place was added to the map. */
    val added: String? get() = addedRaw.nonEmpty()

    /** `YYYY-MM`, the month last eaten there. */
    val visited: String? get() = visitedRaw.nonEmpty()

    val hasVideo: Boolean get() = reel != null

    /** TikTok and Instagram get different wording on the site, so keep them apart. */
    val isTikTok: Boolean
        get() = reel?.let { runCatching { URI(it).host }.getOrNull() }?.contains("tiktok") == true

    /** The price band as the site draws it: € to €€€€, never a star. */
    val priceBand: String get() = "€".repeat(price.coerceIn(1, 4))

    /**
     * The blurb in [lang], falling back the same way the site does: the asked-for
     * language, then English, then whatever translation exists.
     */
    fun blurbIn(lang: String): String =
        blurb[lang] ?: blurb["en"] ?: blurb.values.firstOrNull() ?: ""

    /**
     * Free-text haystack for the search field: name, street and dishes, which is
     * exactly what the site searches.
     */
    fun matches(query: String, lang: String): Boolean {
        if (query.isEmpty()) return true
        val haystack = (listOf(name, address) + mustOrder + blurbIn(lang)).joinToString(" ")
        return haystack.fold().contains(query.fold())
    }
}

/** The site writes "no value" as both a missing key and an empty string, and
 *  means the same thing by each. */
private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotBlank() }

private val combiningMarks = Regex("\\p{Mn}+")

/**
 * Case- and accent-insensitive, so "pohja" finds "Põhja" and "kohvik" finds
 * "Kohvik" — which is how anyone actually types a search.
 *
 * Deliberately not inside a companion: kotlinx.serialization puts the generated
 * `serializer()` on the companion of a @Serializable class, and a private one
 * would hide it from every caller that needs it.
 */
private fun String.fold(): String =
    combiningMarks.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "").lowercase()
