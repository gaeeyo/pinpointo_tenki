package nikeno.Tenki.feature.weather

import nikeno.Tenki.Area
import nikeno.Tenki.Downloader
import java.net.URLEncoder


suspend fun getYahooWeather(downloader: Downloader, url: String): YahooWeather {
    val data: ByteArray = downloader.download(url, 50 * 1024, true)
    return YahooWeatherHtmlParser().parse(data)
}

suspend fun searchAddress(downloader: Downloader, searchText: String): List<Area> {
    val encoding = "utf-8"
    val url = ("https://weather.yahoo.co.jp/weather/search/" + "?p=" + URLEncoder.encode(
        searchText,
        encoding
    ))

    val buff: ByteArray =
        downloader.download(url, 50 * 1024, -1, false) ?: throw Exception("Download error.")

    val html = String(buff, charset(encoding))

    return YahooSearchHtmlParser().parse(html)
}
