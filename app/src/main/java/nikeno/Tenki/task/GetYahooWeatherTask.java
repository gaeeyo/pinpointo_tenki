package nikeno.Tenki.task;

import androidx.annotation.NonNull;

import nikeno.Tenki.Downloader;
import nikeno.Tenki.YahooWeather;

public class GetYahooWeatherTask extends AbstractTask<YahooWeather> {

    @NonNull
    private final String     mUrl;
    @NonNull
    private final Downloader mDownloader;

    public GetYahooWeatherTask(@NonNull Downloader downloader, @NonNull String url,
                               @NonNull Callback<YahooWeather> callback) {
        super(callback);
        mDownloader = downloader;
        mUrl = url;
    }

    @Override
    protected YahooWeather doInBackground() throws Exception {
        byte[] data = mDownloader.download(mUrl, 50 * 1024, true);
        return YahooWeather.parse(data);
    }
}
