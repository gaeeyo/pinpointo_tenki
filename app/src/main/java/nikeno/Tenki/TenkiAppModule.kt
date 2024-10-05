package nikeno.Tenki

import android.app.Application.MODE_PRIVATE
import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.headers
import nikeno.Tenki.feature.cache.ResourceCache
import nikeno.Tenki.feature.fetcher.WeatherFetcher
import nikeno.Tenki.ui.screen.main.MainViewModel
import nikeno.Tenki.ui.screen.selectarea.SelectAreaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

const val CACHE_FILENAME = "file_cache.db"

private const val TAG = "TenkiAppModule"

val tenkiAppModule = module {
    single {
        Log.d(TAG, "HttpClient Creating")
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
        ResourceCache(get(), CACHE_FILENAME)
    }

    single {
        val context: Context = get()
        Prefs(context.getSharedPreferences("AF.Tenki", MODE_PRIVATE))
    }

    single {
        WeatherFetcher(get(), get())
    }

    viewModel { MainViewModel(get()) }
    viewModel { SelectAreaViewModel(get(), get()) }
}

