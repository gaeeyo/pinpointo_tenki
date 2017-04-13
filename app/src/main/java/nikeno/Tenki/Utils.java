package nikeno.Tenki;

public class Utils {
    public static String httpsUrl(String url) {
        if (url.startsWith("http://")) {
            return "https:" + url.substring(5);
        }
        return url;
    }
}
