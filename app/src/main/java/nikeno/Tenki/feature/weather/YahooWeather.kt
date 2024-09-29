package nikeno.Tenki.feature.weather

import nikeno.Tenki.Area
import nikeno.Tenki.Downloader
import nikeno.Tenki.YahooWeather
import java.net.URLEncoder
import java.util.regex.Pattern

suspend fun getYahooWeather(downloader: Downloader, url: String): YahooWeather {
    val data: ByteArray = downloader.download(url, 50 * 1024, true)
    return YahooWeather.parse(data)
}

suspend fun searchAddress(downloader: Downloader, searchText: String): List<Area> {
    val encoding = "utf-8"
    val url = ("https://weather.yahoo.co.jp/weather/search/"
            + "?p=" + URLEncoder.encode(searchText, encoding))

    val buff: ByteArray = downloader.download(url, 50 * 1024, -1, false)
        ?: throw Exception("Download error.")

    val html = String(buff, charset(encoding))

    return parseAreaListHtml_20181112(html)
}

fun parseAreaListHtml_20181112(html: String): ArrayList<Area> {
    var html = html
    val result = ArrayList<Area>()

    html = html.replace("\n", "")

    // ざっくりとした
    val p = Pattern.compile(
        "<thead(.*?)<tfoot",
        Pattern.CASE_INSENSITIVE
    )
    var m = p.matcher(html)
    if (m.find()) {
        html = m.group(1)
        // <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
        m = Pattern.compile(
            "<tr.*?href=\"(.*?)\".*?>(.*?)</a>"
        ).matcher(html)
        while (m.find()) {
            var url = if (m.group(1).startsWith("//")) {
                "https:" + m.group(1)
            } else {
                m.group(1)
            }
            val d = Area(
                "",
                m.group(2),
                "", url!!
            )

            result.add(d)
            //Log.d(TAG, m.group(1) +"," +m.group(2) + "," + m.group(4) + "," + m.group(3) );
        }
    }
    return result
}