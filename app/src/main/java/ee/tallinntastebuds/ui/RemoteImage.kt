package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * A photo from the website's `photos/<id>/` folder.
 *
 * Coil handles the fetch and both its caches; the site serves photos with a
 * week-long `max-age`, so a photo seen once stays on the device without the app
 * keeping a cache of its own. WebP decodes natively on Android, which is the
 * format every photo on the site is in.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val theme = LocalTheme.current
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            is AsyncImagePainter.State.Error -> Box(
                Modifier.fillMaxSize().background(theme.hairline),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = theme.muted,
                    modifier = Modifier.size(22.dp),
                )
            }
            else -> Box(
                Modifier.fillMaxSize().background(theme.hairline),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = theme.muted, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
    }
}
