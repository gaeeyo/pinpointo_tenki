package nikeno.Tenki.feature.weather

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

const val SERVER_ENCODING = "utf-8"
val TZ_TOKYO = TimeZone.of("Asia/Tokyo")


@Immutable
data class YahooWeather(
    val areaName: String, val today: Day, val tomorrow: Day, val days: List<WeeklyDay>
) {
    @Immutable
    data class Hour(
        val hour: Int,
        val text: String,
        val temp: String,
        val humidity: String,
        val rain: String,
        val wind: String,
        val imageUrl: String
    ) {
        fun getImageUrl(enabled: Boolean): String =
            if (enabled) {
                imageUrl
            } else {
                imageUrl.replace(".gif", "_g.gif")
            }

    }

    @Immutable
    data class Day(
        val date: Instant, val hours: List<Hour>
    )

    @Immutable
    data class WeeklyDay(
        val date: String,
        val text: String,
        val imageUrl: String?,
        val tempMax: String,
        val tempMin: String,
        val rain: String,
    )

}

class YahooWeatherParseException internal constructor(val errorCode: Int) : Exception() {
    override val message: String
        get() = "HTMLの処理エラー:$errorCode"

    companion object {
        private const val serialVersionUID = 1L
    }
}

class YahooWeatherHtmlParser {
    fun parse(htmlData: ByteArray?): YahooWeather {
        var html = String(htmlData!!, charset(SERVER_ENCODING))

        html = html.replace("\n", "")

        // 地域名を取得
        val areaName = Regex("<title.*?>(.*?)(（〒.*?）)?の天気.*?</title>")
            .find(html)?.groupValues?.get(1)?.removeTag() ?: throw YahooWeatherParseException(4)

        // 今日と明日の天気を処理
        val dayResults = Regex(
            "<!-- Point -->(.*?)<!-- /Point -->",
            RegexOption.IGNORE_CASE
        ).findAll(html).toList()
        if (dayResults.size != 2) throw YahooWeatherParseException(1)

        val today = parseDay(dayResults[0].groupValues[1])
        val tomorrow = parseDay(dayResults[1].groupValues[1])

        // 週間天気を処理
        val weekResult =
            Regex("\"yjw_table\"(.*?)</table>").find(html) ?: throw YahooWeatherParseException(3)
        val days = parseWeek(weekResult.groupValues[1])

        return YahooWeather(areaName, today, tomorrow, days)
    }

    // 「今日」と「明日」の部分を処理
    @Throws(YahooWeatherParseException::class)
    private fun parseDay(html: String): YahooWeather.Day {
        val pRow = Regex("<tr.*?>(.*?)</tr>", RegexOption.IGNORE_CASE)
        val pColumn = Regex("<td.*?>(.*?)</td>", RegexOption.IGNORE_CASE)
        val pUrl = Regex("(https?://[a-zA-Z0-9./_]*)", RegexOption.IGNORE_CASE)

        val pDate = Regex("yjSt.*?([\\d]+)月[ ]*?([\\d]+)日")
        val dm = pDate.find(html) ?: throw YahooWeatherParseException(25)

        val month = dm.groupValues[1].toInt(10)
        val day = dm.groupValues[2].toInt(10)
        val date = convertDate(month, day)

        val rm = pRow.findAll(html).toList()
        if (rm.size < 6) throw YahooWeatherParseException(10)
        val hours = ArrayList<YahooWeather.Hour>(6)
        val rows = ArrayList<List<String>>()
        for (r in 0..5) {
            val cm = pColumn.findAll(rm[r].groupValues[1]).toList()
            val cols = ArrayList<String>()
            if (cm.size < 9) throw YahooWeatherParseException(11)
            for (c in 0..8) {
                if (c == 0) continue

                cols.add(cm[c].groupValues[1])
            }
            rows.add(cols)
        }

        for (col in 0 until 8) {
            val hour = rows[0][col].removeTag().replace("時", "").trim().toInt()
            val imageUrl = rows[1][col].let { text ->
                val mUrl = pUrl.find(text) ?: throw YahooWeatherParseException(111)
                mUrl.groupValues[1]
            }
            val text = rows[1][col].removeTag()
            val temp = rows[2][col].removeTag()
            val humidity = rows[3][col].removeTag()
            val rain = rows[4][col].removeTag()
            val wind = rows[5][col].removeTag().replace(" ", "")

            hours.add(
                YahooWeather.Hour(
                    hour = hour,
                    text = text,
                    temp = temp,
                    humidity = humidity,
                    rain = rain,
                    wind = wind,
                    imageUrl = imageUrl,
                )
            )
        }
        return YahooWeather.Day(date, hours)
    }

    // 年の情報がない month, date に year を補完する
    private fun convertDate(month: Int, date: Int): Instant {
        val today = Clock.System.now().toLocalDateTime(TZ_TOKYO)
        var year = today.year
        when (today.monthNumber) {
            1 -> if (month == 12) year--
            12 -> if (month == 1) year++
            else -> {}
        }
        return LocalDateTime(year, month, date, 0, 0).toInstant(TimeZone.UTC)
    }

    @Throws(YahooWeatherParseException::class)
    private fun parseWeek(html: String): List<YahooWeather.WeeklyDay> {
        val result = ArrayList<YahooWeather.WeeklyDay>(6)
        val pRow = Regex("<tr.*?>(.*?)</tr>", RegexOption.IGNORE_CASE)
        val pColumn = Regex("<td.*?>(.*?)</td>", RegexOption.IGNORE_CASE)
        val pUrl = Regex("(https?://[a-zA-Z0-9./_]*)", RegexOption.IGNORE_CASE)
        val rm = pRow.findAll(html).map { it.groupValues[1] }.toList()
        if (rm.size <= 3) throw YahooWeatherParseException(20)

        val rows = mutableListOf<List<String>>()
        for (r in 0..3) {
            val cols = pColumn.findAll(rm[r]).map { it.groupValues[1] }.toList()
            if (cols.size <= 6) throw YahooWeatherParseException(21)
            rows.add(cols)
        }
        for (c in 1..6) {
            val date = rows[0][c].removeTag()
            val text = rows[1][c].removeTag()
            // 週間天気予報では7日後の予報が公開されていないことがある
            val imageUrl = rows[1][c].let {
                pUrl.find(it)?.groupValues?.get(1)
            }
            val temp = rows[2][c].split("<br>")
            val tempMax = temp[0].removeTag()
            val tempMin = temp[1].removeTag()
            val rain = rows[3][c].removeTag()
            result.add(YahooWeather.WeeklyDay(date, text, imageUrl, tempMax, tempMin, rain))
        }
        return result
    }

}

private fun _removeTag(html: String): String {
    val p = Regex("<.*?>", RegexOption.IGNORE_CASE)
    return removeBlank(p.replace(html, ""))
}

private fun String.removeTag() = _removeTag(this)

var BLANK_REMOVER = Regex("^\\s*|\\s*$")

private fun removeBlank(html: String): String {
    return BLANK_REMOVER.replace(html, "")
}
