package ee.tallinntastebuds

import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.Strings
import ee.tallinntastebuds.model.Deal
import ee.tallinntastebuds.model.Place
import ee.tallinntastebuds.model.RadioFeed
import ee.tallinntastebuds.model.Taxonomy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The decoders, run against the snapshot that actually ships.
 *
 * These are not tests of the app's own invented data: they read
 * `src/main/assets/seed`, which is a copy of what the website publishes. A
 * change on the site that the models cannot read fails here, on the next
 * snapshot refresh, rather than on a reader's phone.
 */
class ContentTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val seed = File("src/main/assets/seed")

    private fun read(name: String) = File(seed, "$name.json").readText()

    private val places by lazy { json.decodeFromString(ListSerializer(Place.serializer()), read("restaurants")) }
    private val taxonomy by lazy { json.decodeFromString(Taxonomy.serializer(), read("taxonomy")) }
    private val deals by lazy { json.decodeFromString(ListSerializer(Deal.serializer()), read("deals")) }
    private val radio by lazy { json.decodeFromString(RadioFeed.serializer(), read("radio")) }
    private val uiTable by lazy {
        json.decodeFromString(
            MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer())),
            read("ui"),
        )
    }

    @Test
    fun `every place in the snapshot decodes`() {
        assertTrue("the snapshot has no places", places.isNotEmpty())
        assertEquals("two places share an id", places.size, places.map { it.id }.toSet().size)
        assertTrue("a price band is outside 1..4", places.all { it.price in 1..4 })
    }

    @Test
    fun `an empty string means the same as a missing key`() {
        assertTrue(places.none { it.phone?.isBlank() == true })
        assertTrue(places.none { it.reel?.isBlank() == true })
        assertTrue(places.none { it.website?.isBlank() == true })
    }

    @Test
    fun `the discount secret never reaches the model`() {
        // deals.json carries the key each rotating code is derived from. What is
        // never decoded can never be logged or read back out of the app.
        assertTrue(
            Deal::class.java.declaredFields.none { it.name.contains("key", ignoreCase = true) }
        )
    }

    @Test
    fun `every deal names a place on the map`() {
        val ids = places.map { it.id }.toSet()
        assertTrue(deals.all { it.id in ids })
    }

    @Test
    fun `every place type exists in the taxonomy`() {
        val known = taxonomy.types.map { it.id }.toSet()
        val unknown = places.flatMap { it.types }.filterNot { it in known }.toSet()
        assertEquals(emptySet<String>(), unknown)
    }

    @Test
    fun `the taxonomy never claims the reserved discount id`() {
        assertTrue(taxonomy.types.none { it.id == "discount" })
    }

    @Test
    fun `a type chip keeps a label per language and falls back to English`() {
        val type = taxonomy.types.first()
        assertTrue(type.labels.containsKey("en"))
        assertEquals(type.labelIn("en"), type.labelIn("zz"))
    }

    @Test
    fun `search folds case and accents and reaches the dishes`() {
        assertTrue(places.any { it.matches("POHJA", "en") })
        assertTrue(places.any { it.matches("põhja", "en") })
        assertTrue("an empty query must match everything", places.all { it.matches("", "en") })
    }

    @Test
    fun `a blurb follows the language and falls back to English`() {
        val translated = places.first { it.blurb.containsKey("et") && it.blurb.containsKey("en") }
        assertNotEquals(translated.blurbIn("en"), translated.blurbIn("et"))
        assertEquals(translated.blurbIn("en"), translated.blurbIn("zz"))
    }

    @Test
    fun `the radio follows the language and falls back to the default`() {
        assertNotNull(radio.default)
        val russian = radio.byLanguage?.get("ru")
        if (russian != null) assertEquals(russian, radio.stationFor("ru"))
        assertEquals(radio.default, radio.stationFor("no-such-language"))
    }

    @Test
    fun `English leads the language picker`() {
        assertEquals("en", Strings(uiTable, "en").languages.first().code)
    }

    @Test
    fun `placeholders and the separate singular are filled from the site`() {
        val en = Strings(uiTable, "en")
        assertTrue(en("listCount", mapOf("n" to "12")).contains("12"))
        assertNotEquals(en.count(1), en.count(2))
    }

    @Test
    fun `a month renders with the site's own month names`() {
        val en = Strings(uiTable, "en")
        assertTrue(en.monthYear("2026-03").contains("2026"))
        assertEquals("nonsense", en.monthYear("nonsense"))
        assertEquals("2026-13", en.monthYear("2026-13"))
    }

    @Test
    fun `a missing key shows as itself rather than as a blank`() {
        assertEquals("no-such-key", Strings(uiTable, "en")("no-such-key"))
    }

    @Test
    fun `an unknown language falls back to English`() {
        val en = Strings(uiTable, "en")
        assertEquals(en("close"), Strings(uiTable, "zz")("close"))
    }

    /**
     * The site can rename a key at any time. When it does, the app draws the key
     * itself — visible, but wrong — so the list of keys it reads is checked
     * against what the site publishes.
     */
    @Test
    fun `every ui key the app reads is published by the site`() {
        val used = listOf(
            "wordmark", "tagline", "listTitle", "listNew", "listAlphabet", "searchPlaceholder",
            "searchClear", "searchNone", "listCount", "listCountOne", "filterAll", "filterDiscount",
            "noResults", "close", "closed", "closedNote", "address", "phone", "visited", "mustOrder",
            "notFilmed", "directions", "call", "website", "photoOf", "photoClose", "reelPlay",
            "videoPlay", "openPlace", "randomPick", "randomNone", "locate", "locateHere",
            "locateFail", "locateAway", "radioPlay", "radioStop", "radioFail", "language",
            "loadError", "instagramHandle", "months", "monthYear",
        )
        val english = uiTable["en"].orEmpty()
        assertEquals(emptyList<String>(), used.filterNot { english.containsKey(it) })
    }

    /**
     * The app's own furniture is the one thing not published by the site, so it
     * is the one thing that can fall behind the site's language list.
     */
    @Test
    fun `the app's own strings cover every language the site publishes`() {
        val uncovered = uiTable.keys.filter { lang ->
            AppStrings.Key.entries.any { AppStrings.text(it, lang) == it.name }
        }
        assertEquals(emptyList<String>(), uncovered)
    }

    @Test
    fun `an unknown language falls back to English in the app's own strings`() {
        assertEquals(
            AppStrings.text(AppStrings.Key.TAB_MAP, "en"),
            AppStrings.text(AppStrings.Key.TAB_MAP, "zz"),
        )
    }

    @Test
    fun `a price band renders as euro signs and is clamped`() {
        assertEquals("€€€€", places.first { it.price == 4 }.priceBand)
        assertEquals("€", places.first { it.price == 1 }.priceBand)
    }

    @Test
    fun `TikTok is told apart from Instagram`() {
        assertTrue(places.filter { it.hasVideo }.isNotEmpty())
        assertTrue(places.none { !it.hasVideo && it.isTikTok })
        assertNull(places.first { !it.hasVideo }.reel)
    }
}
