package nikeno.Tenki.feature.fetcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.request
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import nikeno.Tenki.Area
import nikeno.Tenki.feature.cache.ResourceCache
import nikeno.Tenki.feature.weather.YahooSearchHtmlParser
import nikeno.Tenki.feature.weather.YahooWeather
import nikeno.Tenki.feature.weather.YahooWeatherHtmlParser
import nikeno.Tenki.feature.weather.YahooWeatherParseException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap

class WeatherFetcher(val client: HttpClient, val cache: ResourceCache) {

    private val mutex = Mutex()
    private val bitmapCache = ConcurrentHashMap<String, Bitmap?>()

    sealed interface WeatherResult {
        data class Success(val data: YahooWeather, val time: Long, val isCache: Boolean) :
            WeatherResult

        data class Error(val error: Throwable) : WeatherResult
    }

    suspend fun getWeather(url: String, since: Long? = null): WeatherResult {
        try {
            if (since != null) {
                try {
                    cache.get(url, since)?.let {
                        return WeatherResult.Success(
                            YahooWeatherHtmlParser().parse(it.data),
                            it.time,
                            true
                        )
                    }
                } catch (e: YahooWeatherParseException) {
                    e.printStackTrace()
                    cache.delete(url)
                }
            }
            val body = client.get(url).body<ByteArray>()
            val weather = YahooWeatherHtmlParser().parse(body)
            val now = Clock.System.now().toEpochMilliseconds()
            cache.put(url, body, now)

            return WeatherResult.Success(weather, now, false)
        } catch (e: Exception) {
            return WeatherResult.Error(e)
        }
    }

    suspend fun getImage2(url: String): Bitmap? {
        bitmapCache.get(url)?.let { return it }

        mutex.withLock {
            bitmapCache[url]?.let { return it }
            try {
                val bmp = downloadImage(url)
                bitmapCache[url] = bmp
                return bmp
            } catch (e: CancellationException) {
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                bitmapCache[url] = null
                return null
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap? {
        cache.get(url, 0)?.let {
            BitmapFactory.decodeByteArray(it.data, 0, it.data.size)?.let {
                it.setDensity(DisplayMetrics.DENSITY_MEDIUM)
                return it
            }
        }
        val data = client.get(url).body<ByteArray>()
        BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
            it.setDensity(DisplayMetrics.DENSITY_MEDIUM)
            cache.put(url, data, Clock.System.now().toEpochMilliseconds())
            return it
        }
        return null
    }


    sealed interface SearchAreaResult {
        data class Success(val list: List<Area>) : SearchAreaResult
        data class Error(val error: Throwable) : SearchAreaResult
    }

    suspend fun searchArea(text: String): SearchAreaResult {
        try {
            val res = client.request("https://weather.yahoo.co.jp/weather/search/") {
                url {
                    parameters.append("p", text)
                }
            }
            val data = YahooSearchHtmlParser().parse(res.body<ByteArray>())
            return SearchAreaResult.Success(data)
        } catch (e: Exception) {
            return SearchAreaResult.Error(e)
        }
    }
}
