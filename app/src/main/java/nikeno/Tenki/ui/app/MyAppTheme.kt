package nikeno.Tenki.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.compose.AppTheme
import nikeno.Tenki.Prefs
import nikeno.Tenki.prefs


@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val currentTheme = context.prefs.theme.collectAsState().value

    val weatherTheme = when (currentTheme) {
        Prefs.ThemeNames.DEFAULT -> WEATHER_THEME_LIGHT
        else -> WEATHER_THEME_DARK
    }

    CompositionLocalProvider(LocalWeatherTheme provides weatherTheme) {
        AppTheme(dynamicColor = true, darkTheme = currentTheme == Prefs.ThemeNames.DARK) {
            content()
        }
    }
}

data class WeatherTheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val borderColor: Color,
    val tableBackground: Color,
    val pastBackground: Color,
    val pastContent: Color,
    val dateBackground: Color,
    val date: Color,
    val hour: Color,
    val weather: Color,
    val temperature: Color,
    val temperatureMax: Color,
    val temperatureMin: Color,
    val humidity: Color,
    val rain: Color,
    val wind: Color,
)

object Light {
    val PRIMARY = Color(0xff000000)
    val SECONDARY = Color(0xff888888)
}

val WEATHER_THEME_LIGHT = WeatherTheme(
    background = Color(0xfff7f7f7),
    primary = Light.PRIMARY,
    secondary = Light.SECONDARY,
    borderColor = Color(0xffaaaaaa),
    tableBackground = Color.White,
    pastBackground = Color(0xffeeeeee),
    pastContent = Color(0xff999999),
    dateBackground = Color(0xffe9eefd),
    date = Light.PRIMARY,
    hour = Light.PRIMARY,
    weather = Light.PRIMARY,
    temperature = Color(0xffff6600),
    temperatureMax = Color(0xffff3300),
    temperatureMin = Color(0xff0066ff),
    humidity = Color(0xff009900),
    rain = Light.PRIMARY,
    wind = Light.PRIMARY
)

object Dark {
    val PRIMARY = Color(0xffbbbbbb)
    val SECONDARY = Color(0xff888888)
}

val WEATHER_THEME_DARK = WeatherTheme(
    background = Color(0xff000000),
    primary = Dark.PRIMARY,
    secondary = Dark.SECONDARY,
    borderColor = Color(0xff4d4d4d),
    tableBackground = Color(0xff131313),
    pastBackground = Color(0xff2b2b2b),
    pastContent = Color(0xff808080),
    dateBackground = Color(0xff343e52),
    date = Dark.PRIMARY,
    hour = Dark.PRIMARY,
    weather = Dark.PRIMARY,
    temperature = Color(0xffcc7832),
    temperatureMax = Color(0xffd96858),
    temperatureMin = Color(0xff589df6),
    humidity = Color(0xff629755),
    rain = Dark.PRIMARY,
    wind = Dark.PRIMARY
)

val LocalWeatherTheme = compositionLocalOf {
    WEATHER_THEME_LIGHT
}
