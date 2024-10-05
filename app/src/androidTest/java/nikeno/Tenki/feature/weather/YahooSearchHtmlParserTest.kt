package nikeno.Tenki.feature.weather

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

class YahooSearchHtmlParserTest {
    @Test
    @Throws(Exception::class)
    fun parse() {
        val html =
            InstrumentationRegistry.getInstrumentation().context.assets.open("search20181112.html")
                .readBytes()
        val areas = YahooSearchHtmlParser().parse(html)
        Assert.assertEquals(6, areas.size.toLong())
        Assert.assertEquals("https://weather.yahoo.co.jp/weather/34/6710/34212.html", areas[0].url)
        Assert.assertEquals("広島県東広島市", areas[0].address1)
    }
}