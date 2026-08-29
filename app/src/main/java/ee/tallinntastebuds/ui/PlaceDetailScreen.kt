package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentSource
import ee.tallinntastebuds.content.ContentState
import ee.tallinntastebuds.model.Deal
import ee.tallinntastebuds.model.Place
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * One place, the way the site's detail panel shows it: photos, the write-up in
 * the reader's language, what to order, and the handful of things you would
 * actually do next — directions, a phone call, the reel, the discount.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    place: Place,
    state: ContentState,
    saved: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    var lightbox by remember(place.id) { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = theme.wash,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.paper),
                navigationIcon = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = state.app(
                                if (saved) AppStrings.Key.SAVED else AppStrings.Key.SAVE
                            ),
                            tint = theme.accent,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = state.strings("close"), tint = theme.ink)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (place.photos.isNotEmpty()) {
                Photos(place, state) { lightbox = it }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(place.name, style = Type.display(26.sp, androidx.compose.ui.text.font.FontWeight.Bold), color = theme.ink)
                Text(
                    text = (listOf(place.priceBand) + state.typeLabels(place)).joinToString(" · "),
                    style = Type.mono(12.sp),
                    color = theme.muted,
                )
            }

            if (place.closed) {
                Text(
                    text = state.strings("closedNote"),
                    style = Type.mono(12.sp),
                    color = theme.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.hairline)
                        .padding(12.dp),
                )
            }

            state.dealFor(place)?.let { deal -> Discount(place, deal, state) }

            ActionRow(place, state)

            Text(
                text = place.blurbIn(state.lang),
                style = Type.running(17.sp),
                color = theme.ink,
            )

            if (place.mustOrder.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(state.strings("mustOrder"))
                    place.mustOrder.forEach { dish ->
                        Text("— $dish", style = Type.running(16.sp), color = theme.ink)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Fact(state.strings("address"), place.address)
                place.phone?.let { Fact(state.strings("phone"), it) }
                place.visited?.let { Fact(state.strings("visited"), state.strings.monthYear(it)) }
                if (place.reel == null) {
                    Text(state.strings("notFilmed"), style = Type.mono(11.sp), color = theme.muted)
                }
            }

            place.reel?.let { reel ->
                WideButton(
                    icon = Icons.Filled.OndemandVideo,
                    label = if (place.isTikTok) state.strings("videoPlay") else state.strings("reelPlay"),
                ) { Actions.openPage(context, reel, theme) }
            }

            MiniMap(place) { Actions.directions(context, place) }
        }
    }

    lightbox?.let { start ->
        PhotoLightbox(place, state, start) { lightbox = null }
    }
}

@Composable
private fun Photos(place: Place, state: ContentState, onOpen: (Int) -> Unit) {
    val pager = rememberPagerState(pageCount = { place.photos.size })
    HorizontalPager(
        state = pager,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) { page ->
        RemoteImage(
            url = ContentSource.photoUrl(place.id, place.photos[page]),
            contentDescription = state.strings(
                "photoOf",
                mapOf("n" to (page + 1).toString(), "total" to place.photos.size.toString()),
            ),
            modifier = Modifier.fillMaxSize().clickable { onOpen(page) },
        )
    }
}

@Composable
private fun Discount(place: Place, deal: Deal, state: ContentState) {
    val theme = LocalTheme.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.paper)
            .border(1.dp, theme.accent, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Label(state.app(AppStrings.Key.DISCOUNT_OFFER))
        Text(deal.offerIn(state.lang), style = Type.display(17.sp), color = theme.ink)
        Text(
            text = state.app(AppStrings.Key.DISCOUNT_OPEN),
            style = Type.display(14.sp),
            color = theme.paper,
            modifier = Modifier
                .clip(shape)
                .background(theme.accent)
                .clickable {
                    // Opened on the site rather than generated here: the code
                    // rotates on a clock the staff verifier shares, and two
                    // implementations of that would be one too many.
                    Actions.openPage(
                        context,
                        ContentSource.dealUrl(place.id, state.lang, theme.isDark),
                        theme,
                    )
                }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ActionRow(place: Place, state: ContentState) {
    val context = LocalContext.current
    // Read here rather than inside a click handler: a composition local is only
    // readable while composing, and a lambda runs long after that.
    val theme = LocalTheme.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SquareAction(
            Icons.Filled.Directions,
            state.strings("directions"),
            Modifier.weight(1f),
        ) { Actions.directions(context, place) }

        place.phone?.let { phone ->
            SquareAction(Icons.Filled.Phone, state.strings("call"), Modifier.weight(1f)) {
                Actions.dial(context, phone)
            }
        }
        place.website?.let { site ->
            SquareAction(Icons.Filled.Public, state.strings("website"), Modifier.weight(1f)) {
                Actions.openPage(context, site, theme)
            }
        }
    }
}

@Composable
private fun SquareAction(icon: ImageVector, title: String, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.paper)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
        Text(title, style = Type.mono(10.sp), color = theme.accent, maxLines = 1)
    }
}

@Composable
private fun WideButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(theme.paper)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = theme.accent, modifier = Modifier.size(20.dp))
        Text(label, style = Type.display(14.sp), color = theme.accent)
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = Type.mono(10.sp), color = LocalTheme.current.muted)
}

@Composable
private fun Fact(label: String, value: String) {
    val theme = LocalTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Label(label)
        Text(value, style = Type.running(15.sp), color = theme.ink)
    }
}

/**
 * A still of where it is. Tapping it hands over to the phone's maps app, which
 * is the only thing anyone wants from a map this small.
 */
@Composable
private fun MiniMap(place: Place, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val context = LocalContext.current

    val mapView = rememberMapView {
        setMultiTouchControls(false)
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        // Not a map to explore — a picture of one. Every gesture belongs to the
        // tap that opens the real thing.
        setOnTouchListener { _, _ -> true }
        controller.setZoom(16.0)
        controller.setCenter(GeoPoint(place.lat, place.lng))
    }

    LaunchedEffect(theme.isDark, place.id) {
        mapView.setTileSource(BaseMap.source(theme.isDark))
        mapView.overlayManager.tilesOverlay.setColorFilter(BaseMap.colorFilter(theme.isDark))
        mapView.overlays.clear()
        mapView.overlays.add(Marker(mapView).apply {
            position = GeoPoint(place.lat, place.lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = pinDrawable(context, theme, place.closed, false)
            setInfoWindow(null)
        })
        mapView.controller.setCenter(GeoPoint(place.lat, place.lng))
        mapView.invalidate()
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    }
}

/** Full-screen photos, the site's lightbox. */
@Composable
fun PhotoLightbox(place: Place, state: ContentState, start: Int, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)

    val pager = rememberPagerState(initialPage = start, pageCount = { place.photos.size })

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            RemoteImage(
                url = ContentSource.photoUrl(place.id, place.photos[page]),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = state.strings("photoClose"), tint = Color.White)
        }

        Text(
            text = state.strings(
                "photoOf",
                mapOf(
                    "n" to (pager.currentPage + 1).toString(),
                    "total" to place.photos.size.toString(),
                ),
            ),
            style = Type.mono(11.sp),
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        )
    }
}
