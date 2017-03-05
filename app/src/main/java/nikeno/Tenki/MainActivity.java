package nikeno.Tenki;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import nikeno.Tenki.view.TextTableView;

public class MainActivity extends Activity {
    public static final String APP_PREF = "AF.Tenki";

    private static final String TAG     = "Tenki_MainActivity";
    private static final String WeekStr = "日月火水木金土";

    static final int REQUEST_AREA = 1;

    private TextView      mTodayHeader;
    private TextView      mTomorrowHeader;
    private TextView      mWeekHeader;
    private TextTableView mTodayTable2;
    private TextTableView mTomorrowTable2;
    private TextTableView mWeekTable2;

    private DownloadTask mDownloadTask;
    private Prefs        mPrefs;
    private String       mPrefUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);

        mTodayHeader = (TextView) findViewById(R.id.todayHeader);
        mTomorrowHeader = (TextView) findViewById(R.id.tomorrowHeader);
        mWeekHeader = (TextView) findViewById(R.id.weekHeader);
        mTodayTable2 = (TextTableView) findViewById(R.id.today2);
        mTomorrowTable2 = (TextTableView) findViewById(R.id.tomorrow2);
        mWeekTable2 = (TextTableView) findViewById(R.id.week2);

        mPrefs = new Prefs(getSharedPreferences(APP_PREF, MODE_PRIVATE));

        if (getIntent().getDataString() != null) {
            mPrefUrl = getIntent().getDataString();
        } else {
            mPrefUrl = mPrefs.getCurrentAreaUrl();
        }
        reload(false);


        // キャッシュを表示
        showCache();
    }

    // ダウンロード
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reload:    // リロード
                reload(true);
                break;
            case R.id.menu_pref:    // 地域変更
                onClickChangeArea(null);
                break;
            case R.id.menu_help:    // ヘルプ
            {
                Intent i = new Intent(getApplicationContext(), HelpActivity.class);
                startActivity(i);
            }
            break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_AREA:
                switch (resultCode) {
                    case RESULT_OK:
                        mPrefUrl = data.getStringExtra("url");
                        mPrefs.setCurrentAreaUrl(mPrefUrl);
                        reload(false);
                        break;
                }
        }
    }

    @Override
    public boolean onSearchRequested() {
        onClickChangeArea(null);
        return true;
    }

    public void onClickChangeArea(View v) {
        Intent i = new Intent(getApplicationContext(), AreaSelectActivity.class);
        startActivityForResult(i, REQUEST_AREA);
    }

    public void onClickOpenBrowser(View v) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mPrefUrl));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCache() {
        byte[] data = Downloader.getInstance(this).getCache(mPrefUrl,
                System.currentTimeMillis() - 24 * DateUtils.HOUR_IN_MILLIS);
        if (data != null) {
            try {
                YahooWeather weatherData = YahooWeather.parse(data);
                setData(weatherData);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, String.valueOf(e.getMessage()));
            }
        }
    }

    // リロードする
    private void reload(boolean force) {
        if (mDownloadTask != null && mDownloadTask.getStatus() != AsyncTask.Status.FINISHED)
            mDownloadTask.cancel(true);

        mDownloadTask = new DownloadTask(force);
        mDownloadTask.execute(mPrefUrl);
    }

    // リロード完了
    private void reloadComplete(DownloadTask task) {
//    	initViews();
        if (task.mResultMessage != null) {
            // エラー表示
            Toast.makeText(getApplicationContext(), task.mResultMessage, Toast.LENGTH_LONG).show();
        } else {
            // エラーなし
            setProgress(75 * 100);
            if (task.isCache()) {
                Toast.makeText(getApplicationContext(),
                        getText(R.string.using_cache), Toast.LENGTH_LONG).show();
            }

            setData(task.mWeatherData);
        }
        setProgress(100 * 100);
        setProgressBarIndeterminateVisibility(false);
    }

    @SuppressWarnings("deprecation")
    public void setDayData(TextTableView v, YahooWeather.Day data, float textSize) {
        Calendar nowJapan = Calendar.getInstance(Locale.JAPAN);

        long      now      = nowJapan.getTime().getTime() - 3 * DateUtils.HOUR_IN_MILLIS;
        long      baseTime = data.date.getTime();
        Resources res      = getResources();

        int columns = data.hours.length;
        v.setSize(columns, 6);

        v.getAll().setTextSize(textSize);
//        v.getRow(1).setIconWidth(res.getDimensionPixelSize(R.dimen.dayWeatherIconWidth));
        v.getRow(2).setTextColor(res.getColor(R.color.tempTextColor));
        v.getRow(3).setTextColor(res.getColor(R.color.humidityTextColor));

        for (int j = 0; j < 6; j++) {
            v.getRow(j).setTextSize(textSize);
        }
        v.getRow(1).setIconWidth((int) (textSize * 2f));

        ImageDownloader downloader          = ImageDownloader.getInstance(this);
        int             dateBgColor         = res.getColor(R.color.tableDateBackgroundColor);
        int             dateBgColorDisabled = res.getColor(R.color.tableDateBackgroundColorDisabled);
        int             textColorDisabled   = res.getColor(R.color.tableTextColorDisabled);

        for (int x = 0; x < columns; x++) {
            YahooWeather.Hour h = data.hours[x];

            boolean enabled  = (baseTime + h.hour * DateUtils.HOUR_IN_MILLIS) > now;
            String  imageUrl = h.getImageUrl(enabled);

            downloader.setImage(imageUrl, new TextTableView.CellBitmapHandler(v, v.getCell(x, 1)));

            v.getCell(x, 0).setText(Integer.toString(h.hour));
            v.getCell(x, 1).setText(h.text);
            v.getCell(x, 2).setText(h.temp);
            v.getCell(x, 3).setText(h.humidity);
            v.getCell(x, 4).setText(h.rain);
            v.getCell(x, 5).setText(h.wind.length() < 3 ?
                    h.wind :
                    h.wind.substring(0, 2) + "\n" + h.wind.substring(2));

            v.getCell(x, 0).setBackgroundColor(enabled ? dateBgColor : dateBgColorDisabled);
            if (!enabled) {
                v.getColumn(x).setTextColor(textColorDisabled);
            }

        }
    }

    public void setWeekData(TextTableView v, YahooWeather.WeeklyDay[] data, float textSize) {
        Resources res          = getResources();
        int       maxTempColor = res.getColor(R.color.maxTempTextColor);
        int       minTempColor = res.getColor(R.color.minTempTextColor);
        boolean   isLandscape  = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        ImageDownloader downloader = ImageDownloader.getInstance(this);
        v.setSize(data.length, 4);
        v.getRow(0).setBackgroundColor(res.getColor(R.color.tableDateBackgroundColor));
//        v.getRow(1).setIconWidth(res.getDimensionPixelSize(R.dimen.weekWeatherIconWidth));
        v.getAll().setTextSize(textSize);
        v.getRow(1).setIconWidth((int) (textSize * 2.5f));
        if (!isLandscape) {
            v.getRow(1).setTextSize(textSize * 0.75f);
        }

        for (int x = 0; x < data.length; x++) {
            YahooWeather.WeeklyDay i = data[x];

            v.getCell(x, 0).setText(isLandscape ? i.date.replace("\n", "") : i.date);

            downloader.setImage(i.imageUrl,
                    new TextTableView.CellBitmapHandler(v, v.getCell(x, 1)));

            v.getCell(x, 1).setText(i.text);

            SpannableString ss = new SpannableString(i.tempMax + "/" + i.tempMin);
            ss.setSpan(new ForegroundColorSpan(maxTempColor),
                    0, i.tempMax.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(minTempColor),
                    ss.length() - i.tempMin.length(), ss.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            v.getCell(x, 2).setText(ss);
            v.getCell(x, 3).setText(i.rain);
        }
    }

    private void setData(YahooWeather data) {
        setTitle(getString(R.string.mainTitleFormat, data.areaName));


        DisplayMetrics dm       = getResources().getDisplayMetrics();
        float          textSize = (dm.widthPixels / 8 / 3);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textSize /= 2;
        }
        mTodayHeader.setText(getDateText(data.today.date));
        mTomorrowHeader.setText(getDateText(data.tomorrow.date));

        mTodayHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTomorrowHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        if (mWeekHeader != null) {
            mWeekHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        setDayData(mTodayTable2, data.today, textSize);
        setDayData(mTomorrowTable2, data.tomorrow, textSize);
        setWeekData(mWeekTable2, data.days, textSize);
    }

    private String getDateText(Date d) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        c.setTime(d);
        int day = c.get(Calendar.DAY_OF_WEEK) - 1;
        return String.format(Locale.ENGLISH, "%d月%d日(%s)",
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DATE),
                WeekStr.substring(day, day + 1));
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private boolean mForceReload;
        private boolean mIsCache       = false;
        public  String  mResultMessage = null;

        public YahooWeather mWeatherData = null;

        public DownloadTask(Boolean force) {
            mForceReload = force;
        }

        public boolean isCache() {
            return mIsCache;
        }

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                publishProgress(25 * 100);
                long since = -1; // リロード
                if (!mForceReload) {
                    // 1時間以内に取得していたらそれを表示
                    since = System.currentTimeMillis() - TenkiApp.PRIORITY_CACHE_TIME;
                }
                // ダウンロード
                byte[] buff = null;
                try {
                    buff = Downloader.getInstance(MainActivity.this)
                            .download(params[0], 50 * 1024, since, true);
                } catch (Exception e) {
                    if (since != -1) {
                        // エラーが発生してもキャッシュがあれば使う
                        buff = Downloader.getInstance(MainActivity.this).getCache(params[0],
                                System.currentTimeMillis() - 8 * DateUtils.HOUR_IN_MILLIS);
                        if (buff != null) {
                            mIsCache = true;
                        }
                    } else {
                        throw e;
                    }
                }
                if (buff == null) {
                    throw new Exception("Download error.");
                }
                mWeatherData = YahooWeather.parse(buff);
                publishProgress(50 * 100);
            } catch (Exception e) {
                mResultMessage = getText(R.string.reload_error) +
                        "\n" + e.getMessage();
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            reloadComplete(this);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            setProgress(values[0]);
        }

    }
}