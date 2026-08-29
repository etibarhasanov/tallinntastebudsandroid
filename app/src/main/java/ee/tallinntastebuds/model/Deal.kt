package ee.tallinntastebuds.model

import kotlinx.serialization.Serializable

/**
 * `data/deals.json`. The rotating code itself is never computed here — the app
 * links out to the website's own discount page, so the one implementation of
 * the code stays where the staff verifier can be sure of it.
 *
 * `deals.json` also carries the secret each code is derived from. It is
 * deliberately not a field on this class: what is never decoded can never be
 * logged, cached in a model, or read out of the app.
 */
@Serializable
data class Deal(
    /** Matches a [Place.id]. */
    val id: String,
    val live: Boolean = false,
    /** The offer text per language, e.g. "15% off your order". */
    val offer: Map<String, String> = emptyMap(),
) {
    fun offerIn(lang: String): String =
        offer[lang] ?: offer["en"] ?: offer.values.firstOrNull() ?: ""
}
