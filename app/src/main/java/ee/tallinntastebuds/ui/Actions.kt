package ee.tallinntastebuds.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import ee.tallinntastebuds.model.Place
import java.net.URLEncoder

/**
 * The handful of places the app hands over to the rest of the phone.
 *
 * Each one is a chain rather than a single intent: Android cannot promise that
 * any particular app is installed, and a button that throws because the reader
 * has no Google Maps is worse than one that falls back to a web page.
 */
object Actions {

    /**
     * Walking directions, because everything on this map is in the old town or a
     * tram ride from it. Turn-by-turn if the phone has something that does it,
     * otherwise a dropped pin, otherwise the browser.
     */
    fun directions(context: Context, place: Place) {
        val label = URLEncoder.encode(place.name, "UTF-8")
        val candidates = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${place.lat},${place.lng}&mode=w")),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}($label)")),
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://www.google.com/maps/dir/?api=1" +
                        "&destination=${place.lat},${place.lng}&travelmode=walking"
                )
            ),
        )
        candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?.let { launch(context, it) }
    }

    /**
     * The dialler with the number in it, not a call being placed. Dialling needs
     * no permission and leaves the last press to the reader, which is the right
     * way round for a number they may only have wanted to look at.
     */
    fun dial(context: Context, phone: String) {
        val digits = phone.filter { it.isDigit() || it == '+' }
        launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
    }

    /**
     * A page from the website, opened in a Custom Tab: the reader's own browser,
     * their own logins, and the app's colours around it.
     *
     * Used for the two things that must stay on the site rather than be
     * reimplemented here — the rotating discount code, which the staff page
     * checks against the same clock, and the Instagram and TikTok posts, which
     * have no embeddable form worth shipping.
     */
    fun openPage(context: Context, url: String, theme: Theme) {
        val intent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(theme.paper.toArgb())
                    .build()
            )
            .setShowTitle(true)
            .build()
        try {
            intent.launchUrl(context, Uri.parse(url))
        } catch (error: ActivityNotFoundException) {
            // No browser at all is rare but possible on a stripped-down device.
            launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            // Nothing to do about it, and nothing worth crashing over: the button
            // simply does not lead anywhere on this phone.
        }
    }
}
