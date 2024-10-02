package nikeno.Tenki.feature.weather

import androidx.test.platform.app.InstrumentationRegistry
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.io.IOException


class YahooWeatherHtmlParserTest {
    @Test
    @Throws(IOException::class)
    fun test20210826() {
        val context = InstrumentationRegistry.getInstrumentation().context

        val html = context.assets.open("yw20210826_13112.html").use {
            it.readBytes()
        }

        val yw2 = YahooWeatherHtmlParser().parse(html)
        println(yw2)
    }

    @Test
    fun testClient() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val client = HttpClient() {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
            install(HttpCache) {
                val path = File(context.filesDir, "http_cache")
                path.mkdirs()
                publicStorage(FileStorage(path))
            }
        }
        runBlocking {
            val req = HttpRequestBuilder().apply {
                url("https://syoboi.jp/")
                headers {
                    append("Cache-Control", "max-age=600")
                }
            }
//            val res = client.get("https://syoboi.jp/")
            val res = client.get(req)
            res.bodyAsText()
            client.close()
        }
    }
}

val x = """
09-30 14:02:39.391 11526 11543 I TestRunner: started: test20210826(nikeno.Tenki.feature.weather.YahooWeatherKtTest)
09-30 14:02:39.413 11526 11543 I System.out: YahooWeather(areaName=世田谷区, today=Day(date=Sun Aug 25 15:00:00 GMT 2024, hours=[Hour(hour=0, text=晴れ, temp=29, humidity=84, rain=0, wind=北2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_night_g.png), Hour(hour=3, text=晴れ, temp=28, humidity=82, rain=0, wind=北北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_night_g.png), Hour(hour=6, text=晴れ, temp=28, humidity=82, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day_g.png), Hour(hour=9, text=晴れ, temp=31, humidity=60, rain=0, wind=北1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_day_g.png), Hour(hour=12, text=晴れ, temp=34, humidity=48, rain=0, wind=南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/21_day_g.png), Hour(hour=15, text=晴れ, temp=35, humidity=48, rain=0, wind=南南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/21_day_g.png), Hour(hour=18, text=曇り, temp=32, humidity=62, rain=0, wind=南南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=21, text=曇り, temp=29, humidity=74, rain=0, wind=南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png)]), tomorrow=Day(date=Mon Aug 26 15:00:00 GMT 2024, hours=[Hour(hour=0, text=曇り, temp=28, humidity=82, rain=0, wind=静穏0, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=3, text=曇り, temp=27, humidity=86, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=6, text=晴れ, temp=27, humidity=84, rain=0, wind=北北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=9, text=晴れ, temp=30, humidity=66, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=12, text=晴れ, temp=32, humidity=55, rain=0, wind=東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=15, text=晴れ, temp=32, humidity=56, rain=0, wind=東南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=18, text=晴れ, temp=30, humidity=63, rain=0, wind=東南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_night.png), Hour(hour=21, text=晴れ, temp=29, humidity=76, rain=0, wind=南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_night.png)]), days=[WeeklyDay(date=28日
09-30 14:02:39.414 11526 11543 I System.out: (土), text=曇のち晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/211_day.png, tempMax=34, tempMin=26, rain=10), WeeklyDay(date=29日
09-30 14:02:39.414 11526 11543 I System.out: (日), text=曇時々晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/201_day.png, tempMax=33, tempMin=26, rain=10), WeeklyDay(date=30日
09-30 14:02:39.414 11526 11543 I System.out: (月), text=曇時々晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/201_day.png, tempMax=32, tempMin=25, rain=20), WeeklyDay(date=31日
09-30 14:02:39.414 11526 11543 I System.out: (火), text=曇時々雨, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/240_day.png, tempMax=30, tempMin=22, rain=50), WeeklyDay(date=1日
09-30 14:02:39.414 11526 11543 I System.out: (水), text=曇り, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/255_day.png, tempMax=28, tempMin=21, rain=40), WeeklyDay(date=2日
09-30 14:02:39.414 11526 11543 I System.out: (木), text=曇り, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/200_day.png, tempMax=29, tempMin=22, rain=20)])
09-30 14:02:39.459 11526 11543 I System.out: YahooWeather(areaName=世田谷区, today=Day(date=2024-08-26, hours=[Hour(hour=0, text=晴れ, temp=29, humidity=84, rain=0, wind=北2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_night_g.png), Hour(hour=3, text=晴れ, temp=28, humidity=82, rain=0, wind=北北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_night_g.png), Hour(hour=6, text=晴れ, temp=28, humidity=82, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day_g.png), Hour(hour=9, text=晴れ, temp=31, humidity=60, rain=0, wind=北1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/10_day_g.png), Hour(hour=12, text=晴れ, temp=34, humidity=48, rain=0, wind=南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/21_day_g.png), Hour(hour=15, text=晴れ, temp=35, humidity=48, rain=0, wind=南南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/21_day_g.png), Hour(hour=18, text=曇り, temp=32, humidity=62, rain=0, wind=南南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=21, text=曇り, temp=29, humidity=74, rain=0, wind=南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png)]), tomorrow=Day(date=2024-08-27, hours=[Hour(hour=0, text=曇り, temp=28, humidity=82, rain=0, wind=静穏0, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=3, text=曇り, temp=27, humidity=86, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/30_night.png), Hour(hour=6, text=晴れ, temp=27, humidity=84, rain=0, wind=北北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=9, text=晴れ, temp=30, humidity=66, rain=0, wind=北東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=12, text=晴れ, temp=32, humidity=55, rain=0, wind=東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=15, text=晴れ, temp=32, humidity=56, rain=0, wind=東南東2, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_day.png), Hour(hour=18, text=晴れ, temp=30, humidity=63, rain=0, wind=東南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_night.png), Hour(hour=21, text=晴れ, temp=29, humidity=76, rain=0, wind=南東1, imageUrl=https://s.yimg.jp/images/weather/general/next/pinpoint/size80/20_night.png)]), days=[WeeklyDay(date=8月28日(土), text=曇のち晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/211_day.png, tempMax=34, tempMin=26, rain=10), WeeklyDay(date=8月29日(日), text=曇時々晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/201_day.png, tempMax=33, tempMin=26, rain=10), WeeklyDay(date=8月30日(月), text=曇時々晴, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/201_day.png, tempMax=32, tempMin=25, rain=20), WeeklyDay(date=8月31日(火), text=曇時々雨, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/240_day.png, tempMax=30, tempMin=22, rain=50), WeeklyDay(date=9月1日(水), text=曇り, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/255_day.png, tempMax=28, tempMin=21, rain=40), WeeklyDay(date=9月2日(木), text=曇り, imageUrl=https://s.yimg.jp/images/weather/general/next/size90/200_day.png, tempMax=29, tempMin=22, rain=20)])
09-30 14:02:39.459 11526 11543 I TestRunner: finished: test20210826(nikeno.Tenki.feature.weather.YahooWeatherKtTest)

"""
