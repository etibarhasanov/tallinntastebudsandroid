package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentState
import ee.tallinntastebuds.content.SortOrder
import ee.tallinntastebuds.model.Place
import ee.tallinntastebuds.service.LocationProvider

/** The site's list panel: search, filter chips, sort, and every place in order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    state: ContentState,
    places: List<Place>,
    savedIds: Set<String>,
    location: LocationProvider,
    locationAvailable: android.location.Location?,
    onOpen: (Place) -> Unit,
    onQuery: (String) -> Unit,
    onToggleType: (String) -> Unit,
    onClearTypes: () -> Unit,
    onSort: (SortOrder) -> Unit,
) {
    val theme = LocalTheme.current
    val requestLocation = rememberLocationRequest(location)

    Scaffold(
        containerColor = theme.wash,
        // The bottom bar is a sibling of this screen, not part of it, so the
        // system navigation inset is already spoken for. Applying it again
        // would leave a strip of empty paper above the tabs.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(state.strings("listTitle"), style = Type.display(17.sp), color = theme.ink) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.paper,
                    titleContentColor = theme.ink,
                ),
                actions = {
                    SortMenu(state) { order ->
                        onSort(order)
                        // Sorting by distance is the one order that needs a fix
                        // to mean anything.
                        if (order == SortOrder.NEAREST) requestLocation()
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(state.query, state.strings("searchPlaceholder"), state.strings("searchClear"), onQuery)
            FilterChips(state, onToggleType, onClearTypes, Modifier.fillMaxWidth())
            HorizontalDivider(color = theme.hairline)

            if (places.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().background(theme.wash).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.query.isEmpty()) state.strings("noResults")
                        else state.strings("searchNone", mapOf("q" to state.query)),
                        style = Type.running(16.sp),
                        color = theme.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(theme.wash),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(places, key = { it.id }) { place ->
                        Box(Modifier.background(theme.paper)) {
                            PlaceRow(
                                place = place,
                                state = state,
                                saved = place.id in savedIds,
                                distanceMetres = distanceFor(state, place, location, locationAvailable),
                                modifier = Modifier.clickable { onOpen(place) },
                            )
                        }
                        HorizontalDivider(color = theme.hairline)
                    }
                    item {
                        Text(
                            text = state.strings.count(places.size),
                            style = Type.mono(11.sp),
                            color = theme.muted,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Only worth showing when it is a distance the reader could act on: from another
 * country it is a number with no meaning.
 */
private fun distanceFor(
    state: ContentState,
    place: Place,
    provider: LocationProvider,
    here: android.location.Location?,
): Float? {
    if (state.sort != SortOrder.NEAREST || provider.isAwayFromTallinn || here == null) return null
    return state.distanceFrom(here, place)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, placeholder: String, clearLabel: String, onQuery: (String) -> Unit) {
    val theme = LocalTheme.current
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text(placeholder, style = Type.running(15.sp), color = theme.muted) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = theme.muted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = clearLabel, tint = theme.muted)
                }
            }
        },
        textStyle = Type.running(15.sp),
        shape = RoundedCornerShape(4.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = theme.paper,
            unfocusedContainerColor = theme.paper,
            focusedTextColor = theme.ink,
            unfocusedTextColor = theme.ink,
            cursorColor = theme.accent,
            focusedIndicatorColor = theme.accent,
            unfocusedIndicatorColor = theme.hairline,
        ),
        modifier = Modifier.fillMaxWidth().background(theme.wash).padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SortMenu(state: ContentState, onSort: (SortOrder) -> Unit) {
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        SortOrder.NEWEST to state.strings("listNew"),
        SortOrder.ALPHABETICAL to state.strings("listAlphabet"),
        SortOrder.NEAREST to state.app(AppStrings.Key.SORT_NEAREST),
    )

    IconButton(onClick = { open = true }) {
        Icon(
            Icons.AutoMirrored.Filled.Sort,
            contentDescription = state.app(AppStrings.Key.SORT),
            tint = theme.ink,
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        options.forEach { (order, label) ->
            DropdownMenuItem(
                text = { Text(label, style = Type.display(14.sp), color = theme.ink) },
                leadingIcon = {
                    RadioButton(selected = state.sort == order, onClick = null, modifier = Modifier.size(20.dp))
                },
                onClick = {
                    open = false
                    onSort(order)
                },
            )
        }
    }
}
