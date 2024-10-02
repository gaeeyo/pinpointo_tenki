package nikeno.Tenki

import android.app.Application.MODE_PRIVATE
import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.headers
import nikeno.Tenki.feature.weather.YahooWeatherClient
import nikeno.Tenki.ui.screen.main.MainViewModel
import nikeno.Tenki.ui.screen.selectarea.SelectAreaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

const val CACHE_FILENAME = "file_cache.db"


val tenkiAppModule = module {
    single {
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
            headers {
                append("User-Agent", "pinpoint_tenki/" + BuildConfig.VERSION_NAME)
            }
        }
    }

    single {
        val context: Context = get()
        Prefs(context.getSharedPreferences("AF.Tenki", MODE_PRIVATE))
    }

    single {
        YahooWeatherClient(get())
    }

    single {
        Downloader(get(), CACHE_FILENAME, get())
    }
    single {
        ImageDownloader(get())
    }

    viewModel { MainViewModel(get()) }
    viewModel { SelectAreaViewModel(get(), get()) }
}

