package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.AppStrings
import ee.tallinntastebuds.content.ContentSource
import ee.tallinntastebuds.content.ContentState

/**
 * What the map is and where it comes from. Nothing to set — language and
 * appearance live on the map itself, next to what they change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(state: ContentState) {
    val theme = LocalTheme.current
    val context = LocalContext.current

    Scaffold(
        containerColor = theme.wash,
        // The bottom bar is a sibling of this screen, not part of it, so the
        // system navigation inset is already spoken for. Applying it again
        // would leave a strip of empty paper above the tabs.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(state.app(AppStrings.Key.ABOUT), style = Type.display(17.sp), color = theme.ink) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.paper,
                    titleContentColor = theme.ink,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(theme.paper).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The painting the map is named for, pulled from the site so
                // that repainting it there repaints it here.
                RemoteImage(
                    url = ContentSource.markUrl,
                    contentDescription = state.strings("wordmark"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(220.dp).height(170.dp).clip(RoundedCornerShape(2.dp)),
                )
                Text(state.strings("wordmark"), style = Type.display(20.sp), color = theme.ink)
                Text(
                    state.strings("tagline"),
                    style = Type.running(14.sp),
                    color = theme.muted,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(color = theme.hairline)

            Column(
                modifier = Modifier.fillMaxWidth().background(theme.paper).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.app(AppStrings.Key.ABOUT_BODY), style = Type.running(16.sp), color = theme.ink)
                Text(state.app(AppStrings.Key.SYNC_NOTE), style = Type.running(13.sp), color = theme.muted)
            }

            HorizontalDivider(color = theme.hairline)

            LinkRow(state.strings("instagramHandle")) {
                Actions.openPage(context, ContentSource.INSTAGRAM_PROFILE, theme)
            }
            HorizontalDivider(color = theme.hairline)
            LinkRow(state.app(AppStrings.Key.OPEN_WEBSITE)) {
                Actions.openPage(context, ContentSource.base, theme)
            }
            HorizontalDivider(color = theme.hairline)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Text(
        text = label,
        style = Type.display(15.sp),
        color = theme.accent,
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.paper)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    )
}
