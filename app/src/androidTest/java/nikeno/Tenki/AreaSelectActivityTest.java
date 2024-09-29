package nikeno.Tenki;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class AreaSelectActivityTest {

    @Test
    public void parseAreaListHtml() throws Exception {
        String html = TestUtils.readAssetFile("search20181112.html");
        ArrayList<Area> areas = SearchAddressTask.parseAreaListHtml_20181112(html);
        Assert.assertEquals(6, areas.size());
        Assert.assertEquals("https://weather.yahoo.co.jp/weather/34/6710/34212.html", areas.get(0).url);
        Assert.assertEquals("広島県東広島市", areas.get(0).address1);
    }
}