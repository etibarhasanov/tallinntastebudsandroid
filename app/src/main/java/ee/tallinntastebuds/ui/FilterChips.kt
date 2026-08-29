package ee.tallinntastebuds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.ContentState

/**
 * The site's filter row: "All" plus one chip per type in use, OR semantics, and
 * the discount chip at the front when there is a live discount.
 */
@Composable
fun FilterChips(
    state: ContentState,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    LazyRow(
        modifier = modifier.background(theme.wash),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Chip(state.strings("filterAll"), state.activeTypes.isEmpty(), onClear)
        }
        items(state.chips, key = { it.id }) { chip ->
            Chip(chip.label, chip.id in state.activeTypes) { onToggle(chip.id) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = label,
        style = Type.display(13.sp, if (selected) FontWeight.SemiBold else FontWeight.Normal),
        color = if (selected) theme.paper else theme.ink,
        modifier = Modifier
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) theme.accent else theme.paper)
            .border(1.dp, if (selected) Color.Transparent else theme.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
