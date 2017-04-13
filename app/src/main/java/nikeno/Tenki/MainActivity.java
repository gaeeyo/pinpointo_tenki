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
import android.view.animation.AnimationUtils;
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
    private TextView      mTime;
    private View          mProgress;
    private View          mErrorGroup;

    private AsyncTask<Void, Void, Object> mDownloadTask;
    private Prefs                         mPrefs;
    private String                        mPrefUrl;
    YahooWeather mData;
    long         mDataTime;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTodayHeader = (TextView) findViewById(R.id.todayHeader);
        mTomorrowHeader = (TextView) findViewById(R.id.tomorrowHeader);
        mWeekHeader = (TextView) findViewById(R.id.weekHeader);
        mTodayTable2 = (TextTableView) findViewById(R.id.today2);
        mTomorrowTable2 = (TextTableView) findViewById(R.id.tomorrow2);
        mWeekTable2 = (TextTableView) findViewById(R.id.week2);
        mTime = (TextView) findViewById(R.id.time);
        mProgress = findViewById(android.R.id.progress);
        mErrorGroup = findViewById(R.id.errorGroup);

        mPrefs = new Prefs(getSharedPreferences(APP_PREF, MODE_PRIVATE));

        if (getIntent().getDataString() != null) {
            mPrefUrl = Utils.httpsUrl(getIntent().getDataString());
        } else {
            mPrefUrl = Utils.httpsUrl(mPrefs.getCurrentAreaUrl());
        }

        loadCache(mPrefUrl);
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
                reload();
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
                        mPrefUrl = Utils.httpsUrl(data.getStringExtra("url"));
                        mPrefs.setCurrentAreaUrl(mPrefUrl);
                        reload();
                        break;
                }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mData != null) {
            setData(mData, mDataTime);
            long ellapsed = System.currentTimeMillis() - mDataTime;
            if (ellapsed < 0 || ellapsed > 5 * DateUtils.MINUTE_IN_MILLIS) {
                reload();
            }
        } else {
            reload();
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

    void loadCache(String url) {
        YahooWeather result = null;
        FileCache.Entry entry = Downloader.getInstance(this).getCache(url,
                System.currentTimeMillis() - 24 * DateUtils.HOUR_IN_MILLIS);
        if (entry != null) {
            try {
                mData = YahooWeather.parse(entry.data);
                mDataTime = entry.time;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, String.valueOf(e.getMessage()));
            }
        }
    }

    // リロードする
    private void reload() {
        if (mDownloadTask != null) {
            return;
        }

        mErrorGroup.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
        mDownloadTask = new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    byte[] buff = Downloader.getInstance(MainActivity.this)
                            .download(mPrefUrl, 50 * 1024, true);
                    if (buff != null) {
                        return YahooWeather.parse(buff);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (o instanceof YahooWeather) {
                    mData = (YahooWeather) o;
                    mDataTime = System.currentTimeMillis();
                    setData(mData, mDataTime);
                } else if (o instanceof Exception) {
                    mErrorGroup.setVisibility(View.VISIBLE);
                    mErrorGroup.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
                }
                onFinish();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                onFinish();
            }

            void onFinish() {
                if (this == mDownloadTask) {
                    mProgress.setVisibility(View.GONE);
                    mDownloadTask = null;
                }
            }

        };
        mDownloadTask.execute();
    }

    @SuppressWarnings("deprecation")
    public void setDayData(TextTableView v, YahooWeather.Day data, float textSize) {

        Calendar nowJapan = Calendar.getInstance(Locale.JAPAN);
        long      now      = nowJapan.getTime().getTime() - 3 * DateUtils.HOUR_IN_MILLIS;
        long      baseTime = data.date.getTime();
        Resources res      = getResources();

        int columns = data.hours.length;
        v.setSize(columns, 6);

//        v.getAll().setTextSize(textSize).setTextColor(res.getColor(R.color.tex))
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
        v.invalidate();
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
    private void setData(YahooWeather data, long time) {


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
        mTime.setText(DateUtils.formatDateTime(this, time,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
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

    public void onClickErrorMessage(View v) {
        reload();
    }
}