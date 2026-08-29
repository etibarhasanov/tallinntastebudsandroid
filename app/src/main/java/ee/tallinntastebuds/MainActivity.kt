package ee.tallinntastebuds

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.tallinntastebuds.content.ContentStore
import ee.tallinntastebuds.ui.RootScreen
import ee.tallinntastebuds.ui.StylePreference
import ee.tallinntastebuds.ui.TasteBudsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val application = application as TasteBudsApplication
        val prefs = application.prefs

        setContent {
            val store: ContentStore = viewModel()
            var style by remember { mutableStateOf(StylePreference.from(prefs.getString(STYLE_KEY, null))) }

            // The bars are transparent either way; what changes is whether the
            // clock on top of them is drawn dark or light.
            LaunchedEffect(style) {
                val bar = if (style.theme.isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
            }

            // Coming back to the app is the natural moment to pick up an edit
            // made on the website in the meantime. Leaving it is the moment to
            // stop asking the phone where it is.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> store.refreshInBackground()
                        Lifecycle.Event.ON_STOP -> application.locationProvider.finish()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            TasteBudsTheme(style.theme) {
                RootScreen(
                    store = store,
                    favourites = application.favourites,
                    location = application.locationProvider,
                    radio = application.radioPlayer,
                    style = style,
                    onStyle = { chosen ->
                        style = chosen
                        prefs.edit().putString(STYLE_KEY, chosen.id).apply()
                    },
                )
            }
        }
    }

    private companion object {
        const val STYLE_KEY = "ttb.style"
    }
}
