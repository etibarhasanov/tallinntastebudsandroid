package ee.tallinntastebuds.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentState
import ee.tallinntastebuds.model.Place
import ee.tallinntastebuds.service.LocationProvider
import ee.tallinntastebuds.service.RadioPlayer
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * The map, which is the site's front page and the app's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    state: ContentState,
    places: List<Place>,
    location: LocationProvider,
    here: android.location.Location?,
    radio: RadioPlayer,
    radioPlaying: Boolean,
    radioFailed: Boolean,
    style: StylePreference,
    onStyle: (StylePreference) -> Unit,
    onLanguage: (String) -> Unit,
    onOpen: (Place) -> Unit,
    onToggleType: (String) -> Unit,
    onClearTypes: () -> Unit,
) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    val requestLocation = rememberLocationRequest(location)

    var toast by remember { mutableStateOf<String?>(null) }
    var awaitingFix by remember { mutableStateOf(false) }

    val mapView = rememberMapView {
        setMultiTouchControls(true)
        // The two-button zoom is a leftover from before pinch worked; it covers
        // the corner of the map and nobody presses it.
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        // Called, not assigned: osmdroid's getter returns a primitive double and
        // its setter takes a boxed Double, so there is no Kotlin property here.
        setMinZoomLevel(3.0)
        setMaxZoomLevel(19.0)
        controller.setZoom(12.6)
        controller.setCenter(GeoPoint(LocationProvider.TALLINN_LAT, LocationProvider.TALLINN_LNG))
    }

    // The basemap follows the style, which is the whole reason the style exists.
    LaunchedEffect(theme.isDark) {
        mapView.setTileSource(BaseMap.source(theme.isDark))
        mapView.overlayManager.tilesOverlay.setColorFilter(BaseMap.colorFilter(theme.isDark))
        mapView.invalidate()
    }

    // Rebuilt whole rather than diffed: seventy markers is nothing to redraw,
    // and a diff would be the only place in the app where a stale pin could hide.
    LaunchedEffect(places, here, theme, state.liveDeals) {
        mapView.overlays.clear()
        places.forEach { place ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(place.lat, place.lng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = pinDrawable(context, theme, place.closed, state.dealFor(place) != null)
                title = place.name
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                    onOpen(place)
                    true
                }
            })
        }
        here?.let { fix ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(fix.latitude, fix.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = hereDrawable(context, theme)
                title = state.strings("locateHere")
                setInfoWindow(null)
                // Not a button. Tapping where you are should do nothing, and
                // swallowing the tap stops it landing on a pin underneath.
                setOnMarkerClickListener { _, _ -> true }
            })
        }
        mapView.invalidate()
    }

    // A stream fails several seconds after the tap that started it, so the
    // message has to wait for the failure rather than be looked for the instant
    // the button is pressed.
    LaunchedEffect(radioFailed) {
        if (radioFailed) {
            toast = state.strings("radioFail")
            radio.clearFailure()
        }
    }

    LaunchedEffect(state.lang) {
        radio.follow(state.radio?.stationFor(state.lang))
    }

    val locationFailed by location.failed.collectAsStateWithLifecycle()
    LaunchedEffect(awaitingFix, here, locationFailed) {
        if (!awaitingFix) return@LaunchedEffect
        when {
            here != null -> {
                awaitingFix = false
                if (location.isAwayFromTallinn) {
                    toast = state.strings("locateAway")
                    mapView.controller.animateTo(
                        GeoPoint(LocationProvider.TALLINN_LAT, LocationProvider.TALLINN_LNG), 12.6, 700L
                    )
                } else {
                    mapView.controller.animateTo(GeoPoint(here.latitude, here.longitude), 15.0, 700L)
                }
            }
            locationFailed -> {
                awaitingFix = false
                toast = state.strings("locateFail")
            }
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3_000)
            toast = null
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playing works either way; without it there is simply no notification. */ }

    Scaffold(
        containerColor = theme.wash,
        // The bottom bar is a sibling of this screen, not part of it, so the
        // system navigation inset is already spoken for. Applying it again
        // would leave a strip of empty paper above the tabs.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(state.strings("wordmark"), style = Type.display(17.sp), color = theme.ink) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.paper,
                    titleContentColor = theme.ink,
                ),
                navigationIcon = { LanguageMenu(state, onLanguage) },
                actions = {
                    AppearanceMenu(state, style, onStyle)
                    RadioButtonAction(state, radioPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !radioPlaying) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        radio.toggle(state.radio?.stationFor(state.lang))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            Column(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                FilterChips(
                    state = state,
                    onToggle = onToggleType,
                    onClear = onClearTypes,
                    modifier = Modifier.fillMaxWidth().background(theme.wash.copy(alpha = 0.94f)),
                )
            }

            Attribution(Modifier.align(Alignment.BottomStart))

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapButton(Icons.Filled.Casino, state.strings("randomPick")) {
                    val pick = state.randomPick(here)
                    if (pick == null) {
                        toast = state.strings("randomNone")
                    } else {
                        mapView.controller.animateTo(GeoPoint(pick.lat, pick.lng), 16.0, 700L)
                        onOpen(pick)
                    }
                }
                MapButton(Icons.Filled.MyLocation, state.strings("locate")) {
                    awaitingFix = true
                    requestLocation()
                }
            }

            AnimatedVisibility(
                visible = toast != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            ) {
                Text(
                    text = toast.orEmpty(),
                    style = Type.mono(12.sp),
                    color = theme.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.paper)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * The tiles are borrowed and the licence asks to be named where the map is, not
 * in an about screen two taps away. Tapping it opens the copyright page.
 */
@Composable
private fun Attribution(modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    Text(
        text = BaseMap.attribution(),
        style = Type.mono(9.sp),
        color = theme.muted,
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(theme.paper.copy(alpha = 0.82f))
            .clickable { Actions.openPage(context, BaseMap.COPYRIGHT_URL, theme) }
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun MapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(theme.paper)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = theme.ink)
        Text(label, style = Type.display(13.sp), color = theme.ink)
    }
}

/**
 * The site keeps language and colour in the rail beside the map, because that is
 * what they change. Same here, rather than a screen away.
 */
@Composable
private fun LanguageMenu(state: ContentState, onLanguage: (String) -> Unit) {
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.Language, contentDescription = state.strings("language"), tint = theme.ink)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        state.strings.languages.forEach { language ->
            DropdownMenuItem(
                text = { Text(language.name, style = Type.display(14.sp), color = theme.ink) },
                leadingIcon = {
                    RadioButton(selected = language.code == state.lang, onClick = null)
                },
                onClick = {
                    open = false
                    onLanguage(language.code)
                },
            )
        }
    }
}

@Composable
private fun AppearanceMenu(
    state: ContentState,
    style: StylePreference,
    onStyle: (StylePreference) -> Unit,
) {
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(
            Icons.Filled.Contrast,
            contentDescription = state.app(AppStrings.Key.APPEARANCE),
            tint = theme.ink,
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        StylePreference.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(state.app(option.labelKey), style = Type.display(14.sp), color = theme.ink) },
                leadingIcon = { RadioButton(selected = option == style, onClick = null) },
                onClick = {
                    open = false
                    onStyle(option)
                },
            )
        }
    }
}

@Composable
private fun RadioButtonAction(state: ContentState, playing: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val station = state.radio?.stationFor(state.lang)
    IconButton(onClick = onClick, enabled = station != null) {
        Icon(
            imageVector = if (playing) Icons.Filled.StopCircle else Icons.Filled.PlayCircleOutline,
            contentDescription = if (playing) state.strings("radioStop") else state.strings("radioPlay"),
            tint = if (station == null) theme.muted else theme.ink,
        )
    }
}
