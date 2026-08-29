package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentState
import ee.tallinntastebuds.model.Place

/**
 * The reader's own shortlist — the one thing in the app that is not a mirror of
 * the website. It lives on the device, in the order the site would list them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    state: ContentState,
    saved: List<Place>,
    onOpen: (Place) -> Unit,
    onUnsave: (Place) -> Unit,
) {
    val theme = LocalTheme.current

    Scaffold(
        containerColor = theme.wash,
        // The bottom bar is a sibling of this screen, not part of it, so the
        // system navigation inset is already spoken for. Applying it again
        // would leave a strip of empty paper above the tabs.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(state.app(AppStrings.Key.TAB_SAVED), style = Type.display(17.sp), color = theme.ink)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.paper,
                    titleContentColor = theme.ink,
                ),
            )
        },
    ) { padding ->
        if (saved.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    tint = theme.muted,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    state.app(AppStrings.Key.SAVED_EMPTY),
                    style = Type.display(17.sp),
                    color = theme.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    state.app(AppStrings.Key.SAVED_EMPTY_HINT),
                    style = Type.running(15.sp),
                    color = theme.muted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(saved, key = { it.id }) { place ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(theme.paper),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            PlaceRow(
                                place = place,
                                state = state,
                                // Every row here is saved by definition, so the
                                // badge would say nothing. The button does.
                                saved = false,
                                modifier = Modifier.clickable { onOpen(place) },
                            )
                        }
                        IconButton(onClick = { onUnsave(place) }, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = state.app(AppStrings.Key.SAVED),
                                tint = theme.accent,
                            )
                        }
                    }
                    HorizontalDivider(color = theme.hairline)
                }
            }
        }
    }
}
