package nikeno.Tenki;


import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nikeno.Tenki.task.SearchAddressTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class YahooWeatherTest {

    public void test20160715() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = InstrumentationRegistry.getContext().getAssets().open("yw20160715.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = new YahooWeather();
        yw.parse(out.toByteArray());
    }

    public void test20161219() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = InstrumentationRegistry.getContext().getAssets().open("yw20161219.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = YahooWeather.parse(out.toByteArray());
        assertEquals("https://s.yimg.jp/images/weather/general/forecast/size45/clouds_sun_st.gif", yw.days[0].imageUrl);
    }

    public void test20170413() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = InstrumentationRegistry.getContext().getAssets().open("yw20170413_13_4410_13103.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = YahooWeather.parse(out.toByteArray());
        assertEquals("https://s.yimg.jp/images/weather/general/forecast/size45/clouds_sun_st.gif", yw.days[0].imageUrl);
    }

    public void test20170413s() throws Exception {
        InputStream is = InstrumentationRegistry.getContext().getAssets().open("search20170413.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);

        String html = new String(out.toByteArray(), "utf-8");
        ArrayList<Area> areas = SearchAddressTask.parseAreaListHtml(html);
        assertTrue(areas != null);
    }


    @Test
    public void test20181020() throws IOException, YahooWeather.YahooWeatherParseException {
        InputStream is = InstrumentationRegistry.getContext().getAssets().open("yw20181020.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);

        String html = new String(out.toByteArray(), "utf-8");
        YahooWeather yw = YahooWeather.parse(out.toByteArray());

        System.out.print(re(html, "<title.*?>(.*?).*</title>"));
    }

    String re(String text, String ptn) {
        Matcher m = Pattern.compile(ptn).matcher(text);
        if (m.find()) {
            return m.group();
        }
        return null;
    }
}
