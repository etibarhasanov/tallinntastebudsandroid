package ee.tallinntastebuds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentStore
import ee.tallinntastebuds.model.Place
import ee.tallinntastebuds.service.Favourites
import ee.tallinntastebuds.service.LocationProvider
import ee.tallinntastebuds.service.RadioPlayer

private enum class Tab(val icon: ImageVector, val label: AppStrings.Key) {
    MAP(Icons.Filled.Map, AppStrings.Key.TAB_MAP),
    LIST(Icons.AutoMirrored.Filled.List, AppStrings.Key.TAB_LIST),
    SAVED(Icons.Filled.Bookmark, AppStrings.Key.TAB_SAVED),
    ABOUT(Icons.Filled.Info, AppStrings.Key.ABOUT),
}

/**
 * The four tabs, and the one detail screen they all open.
 *
 * The detail is held here rather than inside each tab so that a place opened
 * from the map and a place opened from the list are the same screen — closing it
 * puts the reader back where they were, whichever tab that was.
 */
@Composable
fun RootScreen(
    store: ContentStore,
    favourites: Favourites,
    location: LocationProvider,
    radio: RadioPlayer,
    style: StylePreference,
    onStyle: (StylePreference) -> Unit,
) {
    val theme = LocalTheme.current
    val state by store.state.collectAsStateWithLifecycle()
    val savedIds by favourites.ids.collectAsStateWithLifecycle()
    val here by location.location.collectAsStateWithLifecycle()
    val radioPlaying by radio.isPlaying.collectAsStateWithLifecycle()
    val radioFailed by radio.failed.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.MAP) }
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }

    // Looked up rather than stored, so a place edited on the website while its
    // screen is open redraws instead of showing yesterday's copy.
    val opened: Place? = openedId?.let { state.place(it) }

    val visible = remember(state, here) { state.visiblePlaces(here) }

    Box(Modifier.fillMaxSize().background(theme.wash)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.MAP -> MapScreen(
                        state = state,
                        places = visible,
                        location = location,
                        here = here,
                        radio = radio,
                        radioPlaying = radioPlaying,
                        radioFailed = radioFailed,
                        style = style,
                        onStyle = onStyle,
                        onLanguage = store::selectLanguage,
                        onOpen = { openedId = it.id },
                        onToggleType = store::toggleType,
                        onClearTypes = store::clearTypes,
                    )

                    Tab.LIST -> ListScreen(
                        state = state,
                        places = visible,
                        savedIds = savedIds,
                        location = location,
                        locationAvailable = here,
                        onOpen = { openedId = it.id },
                        onQuery = store::setQuery,
                        onToggleType = store::toggleType,
                        onClearTypes = store::clearTypes,
                        onSort = store::setSort,
                    )

                    Tab.SAVED -> SavedScreen(
                        state = state,
                        saved = favourites.filter(state.allPlacesByName, savedIds),
                        onOpen = { openedId = it.id },
                        onUnsave = favourites::toggle,
                    )

                    Tab.ABOUT -> AboutScreen(state)
                }
            }

            NavigationBar(containerColor = theme.paper, contentColor = theme.ink) {
                Tab.entries.forEach { entry ->
                    val label = state.app(entry.label)
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(label, style = Type.display(11.sp)) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.paper,
                            selectedTextColor = theme.accent,
                            indicatorColor = theme.accent,
                            unselectedIconColor = theme.muted,
                            unselectedTextColor = theme.muted,
                        ),
                    )
                }
            }
        }

        if (opened != null) {
            BackHandler { openedId = null }
            PlaceDetailScreen(
                place = opened,
                state = state,
                saved = opened.id in savedIds,
                onSave = { favourites.toggle(opened) },
                onClose = { openedId = null },
            )
        }
    }
}
