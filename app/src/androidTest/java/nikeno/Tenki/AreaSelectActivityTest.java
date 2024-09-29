package nikeno.Tenki;

import static nikeno.Tenki.feature.weather.YahooWeatherKt.parseAreaListHtml_20181112;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class AreaSelectActivityTest {

    @Test
    public void parseAreaListHtml() throws Exception {
        String html = TestUtils.readAssetFile("search20181112.html");
        ArrayList<Area> areas = parseAreaListHtml_20181112(html);
        Assert.assertEquals(6, areas.size());
        Assert.assertEquals("https://weather.yahoo.co.jp/weather/34/6710/34212.html", areas.get(0).getUrl());
        Assert.assertEquals("広島県東広島市", areas.get(0).getAddress1());
    }
}