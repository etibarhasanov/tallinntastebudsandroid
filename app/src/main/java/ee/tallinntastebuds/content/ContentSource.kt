package ee.tallinntastebuds.content

import android.content.SharedPreferences
import java.net.URLEncoder

/**
 * Where the app's content comes from.
 *
 * This single object is the whole reason the app and the website stay in step.
 * Everything the app shows — the places, the type chips, every string of
 * interface text, the discounts, the radio station — is fetched from the same
 * JSON files the website reads. Edit `data/restaurants.json` in the website
 * repo, push, and the change is live in the app the next time it refreshes.
 * No Play Store release is involved.
 */
object ContentSource {

    /**
     * The production host — the site's own domain, the one its canonical tag
     * names. Cloudflare Pages serves the `data` files with `must-revalidate`,
     * so a conditional request is always answered honestly.
     *
     * The project's `tallinntastebuds.pages.dev` address serves the same files
     * from the same deployment and stays valid, so it is the address to fall
     * back to by hand if the domain ever has a bad day.
     */
    const val PRODUCTION_BASE = "https://tallinntastebuds.ee"

    const val BASE_KEY = "ttb.contentBaseURL"

    /**
     * Overridable at launch, so a preview deployment can be pointed at without
     * a rebuild:
     *
     * ```
     * adb shell am start -n ee.tallinntastebuds/.MainActivity
     * # then set ttb.contentBaseURL in the app's shared preferences
     * ```
     *
     * Read once, in [configure], rather than on every access: a base URL that
     * changed underneath a half-finished refresh would mix two sites' content.
     */
    @Volatile
    var base: String = PRODUCTION_BASE
        private set

    fun configure(prefs: SharedPreferences) {
        val override = prefs.getString(BASE_KEY, null)?.trim().orEmpty()
        base = if (override.startsWith("http://") || override.startsWith("https://")) {
            override.trimEnd('/')
        } else {
            PRODUCTION_BASE
        }
    }

    /** The five files the site keeps its content in. */
    enum class Document(val fileName: String) {
        RESTAURANTS("restaurants"),
        TAXONOMY("taxonomy"),
        UI("ui"),
        DEALS("deals"),
        RADIO("radio");

        val path: String get() = "data/$fileName.json"

        /** The copy shipped inside the app, so a first launch with no network
         *  still draws a full map. */
        val seedAsset: String get() = "seed/$fileName.json"
    }

    fun url(document: Document): String = "$base/${document.path}"

    /** Photos live next to the data, one folder per place id. */
    fun photoUrl(placeId: String, file: String): String = "$base/photos/$placeId/$file"

    /**
     * The website's own discount page. The rotating code is generated there and
     * checked by the staff page against the same clock, so the app deliberately
     * does not reimplement it — it opens the page the staff already trust.
     *
     * The place goes in as `r`, which is what `deal.js` reads. `spot` is the map
     * page's parameter and means nothing here: passing it leaves the page with
     * no place at all, and it says so politely instead of failing.
     *
     * `style` only knows the site's two, so the app's pink asks for the site's
     * dark rather than falling through to its light.
     */
    fun dealUrl(placeId: String, lang: String, darkStyle: Boolean): String {
        val query = listOf(
            "r" to placeId,
            "lang" to lang,
            "style" to if (darkStyle) "green" else "red",
        ).joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
        return "$base/deal.html?$query"
    }

    /**
     * The identity mark: a photograph of the watercolour the site is named for.
     * Fetched rather than bundled, so repainting it on the site repaints it here
     * — only the launcher icon has to be a copy, because Android draws that
     * before the app has run.
     */
    val markUrl: String get() = "$base/assets/logo/mark.webp"

    const val INSTAGRAM_PROFILE = "https://www.instagram.com/tallinntastebuds/"
}
