package nikeno.Tenki

import android.app.Application.MODE_PRIVATE
import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import nikeno.Tenki.feature.weather.YahooWeatherClient
import nikeno.Tenki.ui.screen.main.MainViewModel
import nikeno.Tenki.ui.screen.selectarea.SelectAreaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File

const val CACHE_FILENAME = "file_cache.db"


val tenkiAppModule = module {
    single {
        val context: Context = get()
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
            install(HttpCache) {
                val path = File(context.cacheDir, "http_cache")
                publicStorage(FileStorage(path))
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
        Downloader(get(), CACHE_FILENAME)
    }

    viewModel { MainViewModel(get()) }
    viewModel { SelectAreaViewModel(get(), get()) }
}

