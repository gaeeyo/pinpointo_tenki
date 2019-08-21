package nikeno.Tenki.task;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nikeno.Tenki.Area;
import nikeno.Tenki.Downloader;

public class SearchAddressTask extends AbstractTask<List<Area>> {
    private static final String SERVER_ENCODING = "UTF-8";

    @NonNull
    private final String     mText;
    @NonNull
    private final Downloader mDownloader;

    public SearchAddressTask(@NonNull Downloader downloader, @NonNull String text,
                             @NonNull Callback<List<Area>> callback) {
        super(callback);
        mDownloader = downloader;
        mText = text;
    }

    @Override
    List<Area> doInBackground() throws Exception {
        String url = "https://weather.yahoo.co.jp/weather/search/"
                + "?p=" + URLEncoder.encode(mText, SERVER_ENCODING);

        byte[] buff = mDownloader.download(url, 50 * 1024, -1, false);
        if (buff == null) {
            throw new Exception("Download error.");
        }

        String html = new String(buff, SERVER_ENCODING);

        return parseAreaListHtml(html);
    }

    @VisibleForTesting
    public static ArrayList<Area> parseAreaListHtml(String html) throws Exception {
        return parseAreaListHtml_20181112(html);
    }

    @VisibleForTesting
    public static ArrayList<Area> parseAreaListHtml_20181112(String html) throws Exception {
        ArrayList<Area> result = new ArrayList<>();

        html = html.replace("\n", "");

        // ざっくりとした
        Pattern p = Pattern.compile("<thead(.*?)<tfoot",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {

            html = m.group(1);
            // <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
            m = Pattern.compile(
                    "<tr.*?href=\"(.*?)\".*?>(.*?)</a>"
            ).matcher(html);
            while (m.find()) {
                Area d = new Area();
                d.address1 = m.group(2);
                d.address2 = "";
                d.zipCode = "";
                if (m.group(1).startsWith("//")) {
                    d.url = "https:" + m.group(1);
                } else {
                    d.url = m.group(1);
                }
                result.add(d);
                //Log.d(TAG, m.group(1) +"," +m.group(2) + "," + m.group(4) + "," + m.group(3) );
            }
        }
        return result;
    }

    @VisibleForTesting
    public static ArrayList<Area> parseAreaListHtml_old(String html) throws Exception {
        ArrayList<Area> result = new ArrayList<>();

        html = html.replace("\n", "");

        // ざっくりとした祝
        Pattern p = Pattern.compile("<thead.*?郵便番号.*?</thead(.*?)</tbody",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {

            html = m.group(1);
            // <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
            m = Pattern.compile(
                    "<tr.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td.*?>.*?(https://[a-zA-Z0-9./_]*?)\".*?>(.*?)</a>.*?</tr"
            ).matcher(html);
            while (m.find()) {
                Area d = new Area();
                d.address1 = m.group(2);
                d.address2 = m.group(4);
                d.zipCode = m.group(1);
                d.url = m.group(3);
                result.add(d);
                //Log.d(TAG, m.group(1) +"," +m.group(2) + "," + m.group(4) + "," + m.group(3) );
            }
        }
        return result;
    }
}
