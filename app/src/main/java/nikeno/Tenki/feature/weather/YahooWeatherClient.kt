package nikeno.Tenki.feature.weather

import nikeno.Tenki.Area
import nikeno.Tenki.Downloader
import java.net.URLEncoder

class YahooWeatherClient(private val downloader: Downloader) {

    suspend fun getYahooWeather(url: String): YahooWeather {
        val data = downloader.download(url, true)
        return YahooWeatherHtmlParser().parse(data)
    }

    data class CachedWeather(val data: YahooWeather, val time: Long)

    fun getCachedWeather(url: String, since: Long): CachedWeather? {
        val entry = downloader.getCache(url, since) ?: return null
        try {
            return CachedWeather(YahooWeatherHtmlParser().parse(entry.data), entry.time)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun searchAddress(searchText: String): List<Area> {
        val encoding = "utf-8"
        val url = ("https://weather.yahoo.co.jp/weather/search/" + "?p=" + URLEncoder.encode(
            searchText,
            encoding
        ))

        val buff: ByteArray =
            downloader.download(url, false) ?: throw Exception("Download error.")

        val html = String(buff, charset(encoding))

        return YahooSearchHtmlParser().parse(html)
    }
}
