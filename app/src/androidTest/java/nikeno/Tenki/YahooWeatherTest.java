package nikeno.Tenki;


import android.test.InstrumentationTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class YahooWeatherTest extends InstrumentationTestCase {

    public void test20160715() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = getInstrumentation().getContext().getAssets().open("yw20160715.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = new YahooWeather();
        yw.parse(out.toByteArray());
    }

    public void test20161219() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = getInstrumentation().getContext().getAssets().open("yw20161219.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = YahooWeather.parse(out.toByteArray());
        assertEquals("https://s.yimg.jp/images/weather/general/forecast/size45/clouds_sun_st.gif", yw.days[0].imageUrl);
    }

    public void test20170413() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = getInstrumentation().getContext().getAssets().open("yw20170413_13_4410_13103.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = YahooWeather.parse(out.toByteArray());
        assertEquals("https://s.yimg.jp/images/weather/general/forecast/size45/clouds_sun_st.gif", yw.days[0].imageUrl);
    }

    public void test20170413s() throws Exception {
        InputStream is = getInstrumentation().getContext().getAssets().open("search20170413.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);

        String html = new String(out.toByteArray(), "utf-8");
        ArrayList<Area> areas = AreaSelectActivity.parseAreaListHtml(html);
        assertTrue(areas != null);
    }
}
