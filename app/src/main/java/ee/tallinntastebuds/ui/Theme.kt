package ee.tallinntastebuds.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ee.tallinntastebuds.content.AppStrings

/**
 * The app's two styles.
 *
 * Red is the site's own light palette, to the value, from `assets/styles.css`.
 * The dark one is the app's: the site's dark style is a mint green, and this is
 * pink, built on the same eight roles so that every screen keeps working without
 * knowing which style it is drawing.
 */
@Immutable
data class Theme(
    val ink: Color,
    val muted: Color,
    val paper: Color,
    val wash: Color,
    val hairline: Color,
    val accent: Color,
    val accentLit: Color,
    val here: Color,
    val isDark: Boolean,
) {
    companion object {
        val red = Theme(
            ink = Color(0xFF27141A),
            muted = Color(0xFF7D5754),
            paper = Color(0xFFFFF0EA),
            wash = Color(0xFFF7DDD4),
            hairline = Color(0xFFF0CEC3),
            accent = Color(0xFFA81E28),
            accentLit = Color(0xFFC9323D),
            here = Color(0xFF0B62C4),
            isDark = false,
        )

        /**
         * Plum paper under a pink accent. `here` goes cyan for the same reason it
         * goes orange in the site's green: the dot showing where you are has to
         * be the one thing on the map that is not the accent colour.
         */
        val pink = Theme(
            ink = Color(0xFFF6E8EE),
            muted = Color(0xFFBFA2AE),
            paper = Color(0xFF2A1B22),
            wash = Color(0xFF170E13),
            hairline = Color(0xFF443039),
            accent = Color(0xFFF59AC0),
            accentLit = Color(0xFFFFBCD7),
            here = Color(0xFF56C8E0),
            isDark = true,
        )
    }
}

/**
 * The reader's choice of style, persisted. Two, and only two: a third option
 * that follows the phone looks identical to whichever of these the phone is
 * already set to, which is a choice that cannot be seen.
 */
enum class StylePreference(val id: String, val theme: Theme, val labelKey: AppStrings.Key) {
    RED("red", Theme.red, AppStrings.Key.STYLE_RED),

    /** Named in the app's own strings rather than in `ui.json`. The site names
     *  its dark style Green, and this one is not that. */
    PINK("pink", Theme.pink, AppStrings.Key.STYLE_PINK);

    companion object {
        fun from(id: String?): StylePreference = entries.firstOrNull { it.id == id } ?: RED
    }
}

val LocalTheme = staticCompositionLocalOf { Theme.red }

/**
 * The site sets display type in Familjen Grotesk and body in Literata. Neither
 * ships with Android, so the app uses the platform faces at the same weights and
 * keeps the serif for running text, which is the part of the site's voice that
 * actually carries.
 */
object Type {
    fun display(size: TextUnit, weight: FontWeight = FontWeight.SemiBold) =
        TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size)

    fun running(size: TextUnit) =
        TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = size)

    fun mono(size: TextUnit) =
        TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = size)
}

/**
 * Material's own colours are derived from the eight roles rather than chosen
 * separately, so a text field or a menu the app never styles by hand still comes
 * out in the same palette as everything around it.
 */
@Composable
fun TasteBudsTheme(theme: Theme, content: @Composable () -> Unit) {
    val colors = if (theme.isDark) {
        darkColorScheme(
            primary = theme.accent,
            onPrimary = theme.paper,
            secondary = theme.accentLit,
            background = theme.wash,
            onBackground = theme.ink,
            surface = theme.paper,
            onSurface = theme.ink,
            surfaceVariant = theme.hairline,
            onSurfaceVariant = theme.muted,
            outline = theme.hairline,
            outlineVariant = theme.hairline,
        )
    } else {
        lightColorScheme(
            primary = theme.accent,
            onPrimary = theme.paper,
            secondary = theme.accentLit,
            background = theme.wash,
            onBackground = theme.ink,
            surface = theme.paper,
            onSurface = theme.ink,
            surfaceVariant = theme.hairline,
            onSurfaceVariant = theme.muted,
            outline = theme.hairline,
            outlineVariant = theme.hairline,
        )
    }

    CompositionLocalProvider(LocalTheme provides theme) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography(
                bodyLarge = Type.running(16.sp),
                bodyMedium = Type.running(14.sp),
                labelLarge = Type.display(14.sp),
                titleMedium = Type.display(16.sp),
            ),
            content = content,
        )
    }
}
