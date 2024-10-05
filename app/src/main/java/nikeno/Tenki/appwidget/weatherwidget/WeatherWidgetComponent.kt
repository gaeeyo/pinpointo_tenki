package nikeno.Tenki.appwidget.weatherwidget

import nikeno.Tenki.feature.fetcher.WeatherFetcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherWidgetComponent : KoinComponent {
    val fetcher: WeatherFetcher by inject()
}