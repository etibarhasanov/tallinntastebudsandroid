package ee.tallinntastebuds.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ee.tallinntastebuds.BuildConfig
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * The basemap.
 *
 * With a CARTO key the app draws Positron and Dark Matter — the same two sets of
 * tiles the website draws, which is what the two styles were picked against.
 * With no key it draws OpenStreetMap's own, which need none, and inverts them
 * for the dark style because OSM publishes nothing dark and a pale basemap under
 * plum cards is unreadable.
 *
 * The website's key is deliberately not baked in here: it is locked to the
 * site's domain, and a referrer lock means nothing to an app. Put your own in
 * `local.properties` as `ttb.tileKey` — see the README.
 */
object BaseMap {
    val hasKey: Boolean get() = BuildConfig.TILE_KEY.isNotEmpty()

    const val OSM_ATTRIBUTION = "© OpenStreetMap contributors"
    const val CARTO_ATTRIBUTION = "© OpenStreetMap contributors © CARTO"
    const val COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"

    fun attribution(): String = if (hasKey) CARTO_ATTRIBUTION else OSM_ATTRIBUTION

    fun source(dark: Boolean): ITileSource = when {
        hasKey -> Carto(if (dark) "dark_all" else "light_all", BuildConfig.TILE_KEY)
        else -> TileSourceFactory.MAPNIK
    }

    /** Only the borrowed light tiles need turning down; CARTO publishes a real
     *  dark set, and inverting that would come back light. */
    fun colorFilter(dark: Boolean) = if (dark && !hasKey) TilesOverlay.INVERT_COLORS else null

    private class Carto(folder: String, key: String) : OnlineTileSourceBase(
        "carto-$folder",
        0,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/$folder/",
            "https://b.basemaps.cartocdn.com/$folder/",
            "https://c.basemaps.cartocdn.com/$folder/",
        ),
        CARTO_ATTRIBUTION,
    ) {
        private val query = if (key.isEmpty()) "" else "?api_key=$key"

        override fun getTileURLString(pMapTileIndex: Long): String = buildString {
            append(baseUrl)
            append(MapTileIndex.getZoom(pMapTileIndex))
            append('/')
            append(MapTileIndex.getX(pMapTileIndex))
            append('/')
            append(MapTileIndex.getY(pMapTileIndex))
            append(".png")
            append(query)
        }
    }
}

/**
 * A MapView that follows the composition's lifecycle. osmdroid predates Compose
 * and still wants to be told when the screen went away; without that it keeps a
 * tile thread and a file handle alive behind a screen nobody is looking at.
 */
@Composable
fun rememberMapView(configure: MapView.() -> Unit = {}): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply(configure) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}

/**
 * A pin, drawn rather than shipped so that both styles get one in their own
 * accent and neither needs a second copy in the drawables.
 *
 * Deliberately a dot and not a teardrop: the site draws dots, and a teardrop
 * points at a spot half its own height above where the place actually is.
 */
fun pinDrawable(context: Context, theme: Theme, closed: Boolean, hasDeal: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centre = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (hasDeal) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2 * density
        paint.color = theme.accentLit.toArgb()
        canvas.drawCircle(centre, centre, 12 * density, paint)
    }

    paint.style = Paint.Style.FILL
    paint.color = (if (closed) theme.muted else theme.accent).toArgb()
    canvas.drawCircle(centre, centre, 8 * density, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * density
    paint.color = theme.paper.toArgb()
    canvas.drawCircle(centre, centre, 8 * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Where you are. Deliberately not a pin: the site gives "here" a colour of its
 * own — blue beside the red places, cyan beside the pink ones — so that the one
 * dot that is not a recommendation never reads as one.
 */
fun hereDrawable(context: Context, theme: Theme): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centre = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.style = Paint.Style.FILL
    paint.color = theme.here.copy(alpha = 0.20f).toArgb()
    canvas.drawCircle(centre, centre, 16 * density, paint)

    paint.color = theme.here.toArgb()
    canvas.drawCircle(centre, centre, 7.5f * density, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * density
    paint.color = theme.paper.toArgb()
    canvas.drawCircle(centre, centre, 7.5f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}
