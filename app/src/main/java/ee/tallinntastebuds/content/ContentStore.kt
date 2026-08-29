package ee.tallinntastebuds.content

import android.app.Application
import android.content.SharedPreferences
import android.location.Location
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ee.tallinntastebuds.TasteBudsApplication
import ee.tallinntastebuds.model.Deal
import ee.tallinntastebuds.model.Place
import ee.tallinntastebuds.model.RadioFeed
import ee.tallinntastebuds.model.Taxonomy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** How the list and the map order what they show. */
enum class SortOrder { NEWEST, ALPHABETICAL, NEAREST }

/** A filter chip: a taxonomy type, or the one reserved discount id. */
data class Chip(val id: String, val label: String)

/**
 * Everything on screen at one moment: the website's content, and the three
 * choices the reader has made about how to look at it.
 *
 * Immutable, so a screen that reads it cannot be looking at a half-applied
 * refresh. The derived properties are `lazy` rather than functions because a
 * list row asks for [liveDeals] once per row, and rebuilding it seventy times
 * to draw seventy rows would be the one slow thing in the app.
 */
data class ContentState(
    val places: List<Place> = emptyList(),
    val taxonomy: Taxonomy = Taxonomy.empty,
    val deals: List<Deal> = emptyList(),
    val radio: RadioFeed? = null,
    /** The raw `ui.json` table. [strings] is the view onto it. */
    val uiTable: Map<String, Map<String, String>> = emptyMap(),
    val lang: String = Strings.FALLBACK_LANG,
    val query: String = "",
    val activeTypes: Set<String> = emptySet(),
    val sort: SortOrder = SortOrder.NEWEST,
    /** When the content last came off the network. Null means everything on
     *  screen is the cached or seeded copy. */
    val lastSynced: Long? = null,
    val isRefreshing: Boolean = false,
    val lastError: String? = null,
) {
    /** The interface text in the reader's language. */
    val strings: Strings by lazy { Strings(uiTable, lang) }

    fun app(key: AppStrings.Key): String = AppStrings.text(key, lang)

    /** The live discounts, keyed by place id. */
    val liveDeals: Map<String, Deal> by lazy {
        deals.filter { it.live }.associateBy { it.id }
    }

    fun dealFor(place: Place): Deal? = liveDeals[place.id]

    /**
     * The filter chips, in the site's order: the discount first, because it is
     * the only one that is an offer rather than a description, then the taxonomy
     * types that at least one place uses, in taxonomy.json's own order. "All" is
     * drawn by the view ahead of both.
     */
    val chips: List<Chip> by lazy {
        val used = places.flatMap { it.types }.toSet()
        buildList {
            if (liveDeals.isNotEmpty()) add(Chip(DEAL_FILTER, strings("filterDiscount")))
            taxonomy.types
                .filter { it.id in used }
                .forEach { add(Chip(it.id, it.labelIn(lang))) }
        }
    }

    /** The list the map and the list screen both draw, in the reader's order. */
    fun visiblePlaces(near: Location? = null): List<Place> {
        val live = liveDeals
        val filtered = places.filter { place ->
            if (!place.matches(query, lang)) return@filter false
            if (activeTypes.isEmpty()) return@filter true
            // OR semantics, matching the site: any selected chip is enough.
            if (DEAL_FILTER in activeTypes && live[place.id] != null) return@filter true
            place.types.any { it in activeTypes }
        }
        return sorted(filtered, near)
    }

    /**
     * Everything, ignoring the search box and the chips. The saved list is not
     * the place to have a filter quietly applied to it.
     */
    val allPlacesByName: List<Place> by lazy {
        places.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun sorted(input: List<Place>, near: Location?): List<Place> = when (sort) {
        SortOrder.ALPHABETICAL -> input.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOrder.NEWEST -> byNewest(input)
        // No fix yet is not an error — fall back to the default order rather
        // than to an arbitrary one.
        SortOrder.NEAREST -> near?.let { here ->
            input.sortedBy { distanceFrom(here, it) }
        } ?: byNewest(input)
    }

    /**
     * Newest first, with an alphabetical tiebreak so places added on the same day
     * do not shuffle between launches.
     */
    private fun byNewest(input: List<Place>): List<Place> = input.sortedWith(
        compareByDescending<Place> { it.added ?: "" }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )

    fun distanceFrom(location: Location, place: Place): Float {
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, place.lat, place.lng, results)
        return results[0]
    }

    fun place(id: String): Place? = places.firstOrNull { it.id == id }

    fun typeLabels(place: Place): List<String> = place.types.mapNotNull { id ->
        taxonomy.types.firstOrNull { it.id == id }?.labelIn(lang)
    }

    /** The site's dice button: one open place out of whatever is on screen. */
    fun randomPick(near: Location? = null): Place? =
        visiblePlaces(near).filterNot { it.closed }.randomOrNull()

    companion object {
        /**
         * The one chip that is not a taxonomy type. The site reserves this id for
         * the same reason: two chips answering to one id would filter each
         * other's places.
         */
        const val DEAL_FILTER = "discount"
    }
}

/**
 * The app's single source of truth, and a mirror of the website's content.
 */
class ContentStore(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = (application as TasteBudsApplication).prefs
    private val client: ContentClient = (application as TasteBudsApplication).contentClient

    private val _state = MutableStateFlow(ContentState())
    val state: StateFlow<ContentState> = _state.asStateFlow()

    init {
        loadCached()
        // The first refresh is started by the screen, on ON_START, so that
        // coming back to the app and opening it take exactly the same path and
        // a launch does not fire two fetches racing each other.
    }

    // region The reader's choices

    fun selectLanguage(code: String) {
        if (code == _state.value.lang) return
        prefs.edit().putString(LANG_KEY, code).apply()
        _state.update { it.copy(lang = code) }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun toggleType(id: String) = _state.update {
        val next = if (id in it.activeTypes) it.activeTypes - id else it.activeTypes + id
        it.copy(activeTypes = next)
    }

    fun clearTypes() = _state.update { it.copy(activeTypes = emptySet()) }

    fun setSort(order: SortOrder) = _state.update { it.copy(sort = order) }

    // endregion

    // region Loading

    /**
     * Whatever is on disk or in the assets, decoded before the first frame so
     * that frame already has a full map on it.
     */
    private fun loadCached() {
        _state.update {
            it.copy(
                places = client.cached(placesSerializer, ContentSource.Document.RESTAURANTS) ?: emptyList(),
                taxonomy = client.cached(Taxonomy.serializer(), ContentSource.Document.TAXONOMY) ?: Taxonomy.empty,
                deals = client.cached(dealsSerializer, ContentSource.Document.DEALS) ?: emptyList(),
                radio = client.cached(RadioFeed.serializer(), ContentSource.Document.RADIO),
                uiTable = client.cached(uiSerializer, ContentSource.Document.UI) ?: emptyMap(),
                lang = prefs.getString(LANG_KEY, null) ?: preferredLanguage(),
            ).settleLanguage()
        }
    }

    /**
     * A language the site has stopped publishing must not leave the app showing
     * bare keys.
     */
    private fun ContentState.settleLanguage(): ContentState =
        if (uiTable.isEmpty() || uiTable.containsKey(lang)) this
        else copy(lang = Strings.FALLBACK_LANG)

    fun refreshInBackground() {
        viewModelScope.launch { refresh() }
    }

    /**
     * Pull the current content from the site. Each document is independent: a
     * failure on one leaves the others' updates in place, and a 304 leaves the
     * value alone entirely.
     */
    suspend fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        try {
            val (placesResult, typesResult, uiResult, dealsResult, radioResult) = coroutineScope {
                val restaurants = async { fetch(placesSerializer, ContentSource.Document.RESTAURANTS) }
                val types = async { fetch(Taxonomy.serializer(), ContentSource.Document.TAXONOMY) }
                val ui = async { fetch(uiSerializer, ContentSource.Document.UI) }
                val offers = async { fetch(dealsSerializer, ContentSource.Document.DEALS) }
                val stations = async { fetch(RadioFeed.serializer(), ContentSource.Document.RADIO)  }
                Results(restaurants.await(), types.await(), ui.await(), offers.await(), stations.await())
            }

            _state.update { current ->
                // Unchanged is the common case and means what is on screen is
                // already current, so only an update writes anything.
                var next = current
                // An empty places array would be the site having a very bad day,
                // and blanking the map is the one update never worth taking.
                placesResult.updated()?.takeIf { it.isNotEmpty() }?.let { next = next.copy(places = it) }
                typesResult.updated()?.let { next = next.copy(taxonomy = it) }
                uiResult.updated()?.let { next = next.copy(uiTable = it).settleLanguage() }
                dealsResult.updated()?.let { next = next.copy(deals = it) }
                radioResult.updated()?.let { next = next.copy(radio = it) }

                val failed = listOf(placesResult, typesResult, uiResult, dealsResult, radioResult)
                    .any { it is Fetched.Failed }
                if (failed) {
                    next.copy(lastError = next.strings("loadError"))
                } else {
                    next.copy(lastSynced = System.currentTimeMillis(), lastError = null)
                }
            }
        } finally {
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() = _state.update { it.copy(lastError = null) }

    /**
     * One document's outcome. Kept apart from a plain nullable because "the
     * server says nothing changed" and "the fetch failed" must not be confused:
     * the first is success, the second has to surface.
     */
    private sealed interface Fetched<out T> {
        data class Updated<T>(val value: T) : Fetched<T>
        data object Unchanged : Fetched<Nothing>
        data object Failed : Fetched<Nothing>
    }

    /** The new value, or null when the server said nothing changed or the fetch
     *  failed — both of which mean "leave what is on screen alone". */
    private fun <T> Fetched<T>.updated(): T? = when (this) {
        is Fetched.Updated -> value
        Fetched.Unchanged, Fetched.Failed -> null
    }

    /** The five outcomes together, because there is no list that holds five
     *  different `Fetched<T>` without throwing their types away. */
    private data class Results(
        val places: Fetched<List<Place>>,
        val taxonomy: Fetched<Taxonomy>,
        val ui: Fetched<Map<String, Map<String, String>>>,
        val deals: Fetched<List<Deal>>,
        val radio: Fetched<RadioFeed>,
    )

    private suspend fun <T> fetch(
        strategy: DeserializationStrategy<T>,
        document: ContentSource.Document,
    ): Fetched<T> = try {
        client.fetch(strategy, document)?.let { Fetched.Updated(it) } ?: Fetched.Unchanged
    } catch (error: Exception) {
        Fetched.Failed
    }

    // endregion

    /**
     * First launch picks up the phone's language when the site publishes it. The
     * list is a starting guess only — the real set comes from `ui.json`, and
     * `settleLanguage` corrects anything the site does not have.
     */
    private fun preferredLanguage(): String {
        val offered = setOf("en", "et", "ru", "uk", "fi", "az", "pt", "es", "tr")
        val locales = LocaleList.getDefault()
        for (index in 0 until locales.size()) {
            val code = locales[index].language.lowercase()
            if (code in offered) return code
        }
        return Strings.FALLBACK_LANG
    }

    private companion object {
        const val LANG_KEY = "ttb.lang"

        val placesSerializer = ListSerializer(Place.serializer())
        val dealsSerializer = ListSerializer(Deal.serializer())
        val uiSerializer = MapSerializer(
            String.serializer(),
            MapSerializer(String.serializer(), String.serializer()),
        )
    }
}
