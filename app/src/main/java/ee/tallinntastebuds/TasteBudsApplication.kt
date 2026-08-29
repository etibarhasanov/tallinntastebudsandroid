package ee.tallinntastebuds

import android.app.Application
import android.content.Context
import ee.tallinntastebuds.content.ContentClient
import ee.tallinntastebuds.content.ContentSource
import ee.tallinntastebuds.service.Favourites
import ee.tallinntastebuds.service.LocationProvider
import ee.tallinntastebuds.service.RadioPlayer
import org.osmdroid.config.Configuration
import java.io.File

/**
 * The few objects that outlive a screen. They are built here rather than
 * injected because there are five of them and one of each: a container would be
 * more machinery than the thing it contained.
 */
class TasteBudsApplication : Application() {

    /** One file for everything the app remembers: the language, the style, the
     *  saved list, and the ETags. The backup rules name it by this filename. */
    val prefs by lazy { getSharedPreferences("ttb", Context.MODE_PRIVATE) }

    val contentClient by lazy { ContentClient(this, prefs) }
    val favourites by lazy { Favourites(prefs) }
    val locationProvider by lazy { LocationProvider(this) }
    val radioPlayer by lazy { RadioPlayer(this) }

    override fun onCreate() {
        super.onCreate()
        ContentSource.configure(prefs)
        configureMap()
    }

    /**
     * osmdroid writes its tile cache wherever it is told, and its default is a
     * shared folder on external storage that modern Android will not grant. Both
     * paths go inside the app's own cache, where no permission is involved and
     * uninstalling actually removes them.
     */
    private fun configureMap() {
        Configuration.getInstance().apply {
            load(this@TasteBudsApplication, prefs)
            // OpenStreetMap's tile policy asks that an app identify itself, and
            // refuses the default "osmdroid" outright.
            userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
            osmdroidBasePath = File(cacheDir, "osmdroid").apply { mkdirs() }
            osmdroidTileCache = File(osmdroidBasePath, "tiles").apply { mkdirs() }
        }
    }
}
