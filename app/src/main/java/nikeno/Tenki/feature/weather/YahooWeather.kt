package nikeno.Tenki.feature.weather

import nikeno.Tenki.Downloader
import nikeno.Tenki.YahooWeather

suspend fun getYahooWeather(downloader: Downloader, url: String): YahooWeather {
    val data: ByteArray = downloader.download(url, 50 * 1024, true)
    return YahooWeather.parse(data)
}