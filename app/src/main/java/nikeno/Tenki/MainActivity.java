package nikeno.Tenki;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import nikeno.Tenki.activity.AreaSelectActivity;
import nikeno.Tenki.activity.HelpActivity;
import nikeno.Tenki.db.entity.ResourceCacheEntity;
import nikeno.Tenki.dialog.DisplaySettingsDialog;
import nikeno.Tenki.task.Callback;
import nikeno.Tenki.task.GetYahooWeatherTask;
import nikeno.Tenki.view.TextTableView;
import nikeno.Tenki.viewbinding.ActivityMainBinding;

public class MainActivity extends Activity implements DisplaySettingsDialog.Listener {
    static final         int    REQUEST_AREA = 1;
    private static final String TAG          = "Tenki_MainActivity";
    private static final String WeekStr      = "日月火水木金土";
    YahooWeather mData;
    long         mDataTime;
    int          mColorTempText;
    int          mColorHumidityText;
    int          mColorDateBg;
    int          mColorDateBgDisabled;
    int          mColorTextDisabled;
    int          mColorMaxTempText;
    int          mColorMinTempText;

    private GetYahooWeatherTask mDownloadTask;
    private Prefs               mPrefs;
    private String              mPrefUrl;
    private ActivityMainBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TenkiApp.applyActivityTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mBinding = new ActivityMainBinding(this);
        mPrefs = ((TenkiApp) getApplication()).getPrefs();

        if (getIntent().getDataString() != null) {
            mPrefUrl = Utils.httpsUrl(getIntent().getDataString());
        } else {
            mPrefUrl = Utils.httpsUrl(mPrefs.getCurrentAreaUrl());
        }

        TypedArray ta = getTheme().obtainStyledAttributes(R.styleable.WeatherTable);
        mColorTempText = ta.getColor(R.styleable.WeatherTable_colorTempText, 0);
        mColorHumidityText = ta.getColor(R.styleable.WeatherTable_colorHumidityText, 0);
        mColorDateBg = ta.getColor(R.styleable.WeatherTable_colorTableDateBackground, 0);
        mColorDateBgDisabled = ta.getColor(R.styleable.WeatherTable_colorTableDateBackgroundDisabled, 0);
        mColorTextDisabled = ta.getColor(R.styleable.WeatherTable_colorTableTextDisabled, 0);
        mColorMaxTempText = ta.getColor(R.styleable.WeatherTable_colorMaxTempText, 0);
        mColorMinTempText = ta.getColor(R.styleable.WeatherTable_colorMinTempText, 0);
        ta.recycle();

        loadCache(mPrefUrl);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mi = menu.findItem(R.id.darkTheme);
        if (mi != null) mi.setChecked(mPrefs.getTheme() == Prefs.ThemeNames.DARK);
        return super.onPrepareOptionsMenu(menu);
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
            case R.id.display_settings:
//                showDisplaySettings();
                new DisplaySettingsDialog(this).show();
                break;
            case R.id.darkTheme:
                mPrefs.setTheme(item.isChecked() ? Prefs.ThemeNames.DEFAULT : Prefs.ThemeNames.DARK);
                recreate();
                break;
        }
        return true;
    }

    private void showDisplaySettings() {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.close, null)
                .create();
        dlg.setOwnerActivity(this);
        dlg.setTitle(R.string.display_settings);
        dlg.setView(dlg.getLayoutInflater().inflate(R.layout.display_settings, null));
        CheckBox showWeatherIcon = dlg.findViewById(R.id.show_weather_icon);
        showWeatherIcon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getParent();
            }
        });
        dlg.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_AREA:
                if (resultCode == RESULT_OK) {
                    mPrefUrl = Utils.httpsUrl(data.getStringExtra("url"));
                    mPrefs.setCurrentAreaUrl(mPrefUrl);
                    reload();
                }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mData != null) {
            setData(mData, mDataTime);
            long elapsed = System.currentTimeMillis() - mDataTime;
            if (elapsed < 0 || elapsed > 5 * DateUtils.MINUTE_IN_MILLIS) {
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
        ResourceCacheEntity entry = TenkiApp.from(this).getDownloader().getCache(url,
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

        mBinding.errorGroup.setVisibility(View.GONE);
        mBinding.progress.setVisibility(View.VISIBLE);
        mDownloadTask = new GetYahooWeatherTask(
                TenkiApp.from(this).getDownloader(),
                mPrefUrl, new Callback<YahooWeather>() {
            @Override
            public void onSuccess(YahooWeather result) {
                mData = result;
                mDataTime = System.currentTimeMillis();
                setData(mData, mDataTime);
            }

            @Override
            public void onError(Throwable error) {
                mBinding.errorGroup.setVisibility(View.VISIBLE);
                mBinding.errorGroup.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_in));
            }

            @Override
            public void onFinish() {
                super.onFinish();
                mBinding.progress.setVisibility(View.GONE);
                mDownloadTask = null;
            }
        });
        mDownloadTask.execute();
    }

    public void setDayData(TextTableView v, YahooWeather.Day data, float textSize) {

        Calendar nowJapan = Calendar.getInstance(Locale.JAPAN);
        long     now      = nowJapan.getTime().getTime() - 3 * DateUtils.HOUR_IN_MILLIS;
        long     baseTime = data.date.getTime();

        boolean showWeatherIcon      = mPrefs.get(Prefs.SHOW_WEATHER_ICON);
        boolean showWeatherIconLabel = mPrefs.get(Prefs.SHOW_WEATHER_ICON_LABEL);
        boolean showTemperature      = mPrefs.get(Prefs.SHOW_TEMPERATURE);
        boolean showHumidity         = mPrefs.get(Prefs.SHOW_HUMIDITY);
        boolean showPrecipitation    = mPrefs.get(Prefs.SHOW_PRECIPITATION);
        boolean showWind    = mPrefs.get(Prefs.SHOW_WIND);

        int columns = data.hours.length;

        int r                = 1;
        int timeRow          = 0;
        int weatherRow       = ((showWeatherIcon || showWeatherIconLabel) ? r++ : -1);
        int tempRow          = (showTemperature ? r++ : -1);
        int humidityRow      = (showHumidity ? r++ : -1);
        int precipitationRow = (showPrecipitation ? r++ : -1);
        int windRow          = (showWind ? r++ : -1);
        v.setSize(columns, r);

        v.getRow(timeRow).setTextSize(textSize);  // 時間
        if (weatherRow != -1) {
            v.getRow(weatherRow).setTextSize(textSize * 0.75f);  // 天気
        }
        if (tempRow != -1) {
            v.getRow(tempRow).setTextSize(textSize);  // 温度
            v.getRow(tempRow).setTextColor(mColorTempText);
        }
        if (humidityRow != -1) {
            v.getRow(humidityRow).setTextSize(textSize);  // 湿度
            v.getRow(humidityRow).setTextColor(mColorHumidityText);
        }
        if (precipitationRow != -1) {
            v.getRow(precipitationRow).setTextSize(textSize);  // 雨量
        }
        if (windRow != -1) {
            v.getRow(windRow).setTextSize(textSize * 0.75f);  // 風
        }

        v.getRow(1).setIconWidth((int) (textSize * 2f));

        ImageDownloader downloader = ImageDownloader.getInstance(this);

        for (int x = 0; x < columns; x++) {
            YahooWeather.Hour h = data.hours[x];

            boolean enabled  = (baseTime + h.hour * DateUtils.HOUR_IN_MILLIS) > now;
            String  imageUrl = h.getImageUrl(enabled);

            v.getCell(x, timeRow).setText(Integer.toString(h.hour));

            if (weatherRow != -1 && showWeatherIcon) {
                downloader.setImage(imageUrl,
                        new TextTableView.CellBitmapHandler(v, v.getCell(x, weatherRow)));
            }
            if (weatherRow != -1 && showWeatherIconLabel) v.getCell(x, weatherRow).setText(h.text);
            if (tempRow != -1) v.getCell(x, tempRow).setText(h.temp);
            if (humidityRow != -1) v.getCell(x, humidityRow).setText(h.humidity);
            if (precipitationRow != -1) v.getCell(x, precipitationRow).setText(h.rain);
            if (windRow != -1) {
                v.getCell(x, windRow).setText(h.wind.length() < 3 ?
                        h.wind :
                        h.wind.substring(0, 2) + "\n" + h.wind.substring(2));
            }

            v.getCell(x, timeRow).setBackgroundColor(enabled ? mColorDateBg : mColorDateBgDisabled);
            if (!enabled) {
                v.getColumn(x).setTextColor(mColorTextDisabled);
            }
        }
        v.requestLayout();
        v.invalidate();
    }


    public void setWeekData(TextTableView v, YahooWeather.WeeklyDay[] data, float textSize) {
        Resources res         = getResources();
        boolean   isLandscape = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        ImageDownloader downloader = ImageDownloader.getInstance(this);
        v.setSize(data.length, 4);
        v.getRow(0).setBackgroundColor(mColorDateBg);
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
            ss.setSpan(new ForegroundColorSpan(mColorMaxTempText),
                    0, i.tempMax.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(mColorMinTempText),
                    ss.length() - i.tempMin.length(), ss.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            v.getCell(x, 2).setText(ss);
            v.getCell(x, 3).setText(i.rain);
        }
    }

    private YahooWeather mLastData;
    private long         mLastDataTime;

    private void setData(YahooWeather data, long time) {


        setTitle(getString(R.string.mainTitleFormat, data.areaName));

        mLastData = data;
        mLastDataTime = time;

        DisplayMetrics dm       = getResources().getDisplayMetrics();
        float          textSize = (dm.widthPixels / 8f / 3f);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textSize /= 2;
        }
        mBinding.todayHeader.setText(getDateText(data.today.date));
        mBinding.tomorrowHeader.setText(getDateText(data.tomorrow.date));

        mBinding.todayHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mBinding.tomorrowHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        if (mBinding.weekHeader != null) {
            mBinding.weekHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        setDayData(mBinding.todayTable2, data.today, textSize);
        setDayData(mBinding.tomorrowTable2, data.tomorrow, textSize);
        setWeekData(mBinding.weekTable2, data.days, textSize);
        mBinding.time.setText(DateUtils.formatDateTime(this, time,
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

    @Override
    public void onDisplaySettingChanged() {
        if (mLastData != null) {
            setData(mLastData, mLastDataTime);
        }
    }
}