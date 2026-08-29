package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentSource
import ee.tallinntastebuds.content.ContentState
import ee.tallinntastebuds.model.Place
import java.util.Locale

/**
 * One line in the list: the name, the price band, what it is good for, and the
 * bookmark that says the reader has kept it.
 */
@Composable
fun PlaceRow(
    place: Place,
    state: ContentState,
    saved: Boolean,
    distanceMetres: Float? = null,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Thumbnail(place, theme)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.name,
                    style = Type.display(16.sp),
                    color = if (place.closed) theme.muted else theme.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (saved) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = state.app(AppStrings.Key.SAVED),
                        tint = theme.accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Text(
                text = subtitle(place, state, distanceMetres),
                style = Type.mono(11.sp),
                color = theme.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            state.dealFor(place)?.let { deal ->
                Text(
                    text = deal.offerIn(state.lang),
                    style = Type.mono(11.sp),
                    color = theme.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (place.closed) {
                Text(state.strings("closed"), style = Type.mono(11.sp), color = theme.muted)
            }
        }
    }
}

@Composable
private fun Thumbnail(place: Place, theme: Theme) {
    val shape = RoundedCornerShape(4.dp)
    val box = Modifier
        .size(56.dp)
        .clip(shape)
        .alpha(if (place.closed) 0.5f else 1f)

    val file = place.photos.firstOrNull()
    if (file != null) {
        RemoteImage(
            url = ContentSource.photoUrl(place.id, file),
            contentDescription = null,
            modifier = box,
        )
    } else {
        // No photo yet, so the band does the job a photo would: it says
        // something about the place at a glance.
        Box(box.background(theme.hairline), contentAlignment = Alignment.Center) {
            Text(place.priceBand, style = Type.mono(13.sp), color = theme.muted)
        }
    }
}

/** "€€ · Coffee, Laptop friendly · 400 m" */
private fun subtitle(place: Place, state: ContentState, distanceMetres: Float?): String {
    val parts = mutableListOf(place.priceBand)
    val types = state.typeLabels(place)
    if (types.isNotEmpty()) parts += types.take(2).joinToString(", ")
    distanceMetres?.let { parts += formatDistance(it) }
    return parts.joinToString(" · ")
}

/**
 * Metres up to a kilometre, then kilometres to one decimal. Rounder than that
 * and a walk of four hundred metres reads as "0.4 km", which nobody says.
 */
fun formatDistance(metres: Float): String = if (metres < 1000f) {
    String.format(Locale.getDefault(), "%.0f m", metres)
} else {
    String.format(Locale.getDefault(), "%.1f km", metres / 1000f)
}
