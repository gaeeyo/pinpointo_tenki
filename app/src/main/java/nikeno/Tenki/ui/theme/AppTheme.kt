package nikeno.Tenki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import nikeno.Tenki.Prefs
import nikeno.Tenki.R
import nikeno.Tenki.TenkiApp


@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val currentTheme = TenkiApp.from(context).prefs.theme
    val colorScheme = when (currentTheme) {
        Prefs.ThemeNames.DEFAULT -> lightColorScheme(
            surface = colorResource(
                R.color.window_background_color_light
            ),
            primary = Color.Black,
            secondary = Color.Gray
        )

        else -> darkColorScheme(surface = colorResource(R.color.window_background_color_dark))
    }

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}