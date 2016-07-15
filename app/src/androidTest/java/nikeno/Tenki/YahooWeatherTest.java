package nikeno.Tenki;


import android.test.InstrumentationTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class YahooWeatherTest extends InstrumentationTestCase {

    public void test20160715() throws IOException, YahooWeather.YahooWeatherParseException {

        InputStream is = getInstrumentation().getContext().getAssets().open("yw20160715.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) out.write(c);
        YahooWeather yw = new YahooWeather();
        yw.parse(out.toByteArray());
    }
}
