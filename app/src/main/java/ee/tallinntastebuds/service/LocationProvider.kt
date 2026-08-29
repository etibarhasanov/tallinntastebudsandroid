package ee.tallinntastebuds.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A thin wrapper over the platform's LocationManager: one fix when asked, never
 * a running trace. The app only needs to know roughly where you are to sort by
 * distance and to centre the map, so it asks once and stops the moment it has an
 * answer.
 *
 * Deliberately not Google's fused provider. That would be a dependency on Play
 * Services for a feature the app can live without, and it would leave the map
 * unable to find you on a phone that has no Google on it at all.
 */
class LocationProvider(context: Context) {

    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    /** True once an attempt has finished with nothing to show for it. Reset by
     *  the next [request], so a refused fix does not haunt the button forever. */
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var listener: LocationListener? = null

    val hasPermission: Boolean
        get() = PERMISSIONS.any {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Far enough outside the city that sorting by distance stops meaning
     * anything — the site shows every place rather than a nearest list.
     */
    val isAwayFromTallinn: Boolean
        get() = _location.value?.let { here ->
            val results = FloatArray(1)
            Location.distanceBetween(here.latitude, here.longitude, TALLINN_LAT, TALLINN_LNG, results)
            results[0] > AWAY_THRESHOLD_METRES
        } ?: false

    /**
     * Ask once. Safe to call repeatedly — this only ever starts one listener, and
     * a second call while the first is outstanding is ignored rather than
     * doubled.
     */
    @SuppressLint("MissingPermission")
    fun request() {
        _failed.value = false
        if (!hasPermission || manager == null) {
            _failed.value = true
            return
        }
        if (listener != null) return

        val provider = bestProvider()
        if (provider == null) {
            // Every provider is switched off. Location is off on the phone, not
            // broken in the app, and the message the reader gets is the same.
            _failed.value = true
            return
        }

        // A recent fix the system already has beats waiting for a new one, and
        // for centring a city map it is just as good. The live request below
        // still runs and replaces it the moment something better arrives.
        lastKnown()?.let { _location.value = it }

        val single = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _location.value = location
                _failed.value = false
                finish()
            }

            // Removed in API 30 but still abstract on older platforms, so both
            // have to be here for this to compile against either.
            @Deprecated("Required by the pre-API-30 interface")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

            override fun onProviderDisabled(provider: String) {
                if (_location.value == null) _failed.value = true
                finish()
            }

            override fun onProviderEnabled(provider: String) = Unit
        }
        listener = single
        manager.requestLocationUpdates(provider, 0L, 0f, single, Looper.getMainLooper())

        // Nothing about a location request promises to come back. Without this,
        // a provider that never answers leaves the button spinning for good.
        handler.postDelayed({
            if (listener === single) {
                if (_location.value == null) _failed.value = true
                finish()
            }
        }, TIMEOUT_MS)
    }

    /** Stop listening. Called when the app goes to the background, so a request
     *  left outstanding is not a location trace by accident. */
    fun finish() {
        listener?.let { manager?.removeUpdates(it) }
        listener = null
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(): Location? {
        val cutoff = System.currentTimeMillis() - LAST_KNOWN_MAX_AGE_MS
        return manager?.getProviders(true).orEmpty()
            .mapNotNull { runCatching { manager?.getLastKnownLocation(it) }.getOrNull() }
            .filter { it.time > cutoff }
            .maxByOrNull { it.time }
    }

    /**
     * Network first: the app wants a hundred metres, not a hundred centimetres,
     * and the network provider answers in a second where GPS can take thirty.
     */
    private fun bestProvider(): String? {
        val enabled = manager?.getProviders(true).orEmpty()
        val preferred = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return preferred.firstOrNull { it in enabled }
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        /** Tallinn, roughly the middle of the map, used until a fix arrives. */
        const val TALLINN_LAT = 59.437
        const val TALLINN_LNG = 24.7536

        const val AWAY_THRESHOLD_METRES = 60_000f

        private const val TIMEOUT_MS = 12_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
