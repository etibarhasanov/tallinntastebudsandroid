package ee.tallinntastebuds.service

import android.content.SharedPreferences
import ee.tallinntastebuds.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one piece of content the app owns rather than mirrors: a reader's own
 * shortlist, kept on the device. The website has no accounts, so this stays
 * local — nothing to sign into, nothing to leak.
 */
class Favourites(private val prefs: SharedPreferences) {

    private val _ids = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun contains(place: Place): Boolean = place.id in _ids.value

    fun toggle(place: Place) {
        val next = _ids.value.let { if (place.id in it) it - place.id else it + place.id }
        _ids.value = next
        // A new set each time: SharedPreferences documents that the set handed to
        // putStringSet must not be mutated afterwards, and reusing one is the
        // classic way to lose the write.
        prefs.edit().putStringSet(KEY, HashSet(next)).apply()
    }

    /**
     * Kept in the order the store hands them over, so a saved list reads the same
     * way as the list it was saved from.
     */
    fun filter(places: List<Place>, ids: Set<String>): List<Place> =
        places.filter { it.id in ids }

    private companion object {
        const val KEY = "ttb.favourites"
    }
}
