package nikeno.Tenki.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import nikeno.Tenki.Prefs;
import nikeno.Tenki.R;
import nikeno.Tenki.TenkiApp;
import nikeno.Tenki.YahooWeather;
import nikeno.Tenki.appwidget.weatherwidget.WeatherIconManager;
import nikeno.Tenki.appwidget.weatherwidget.WidgetTheme;
import nikeno.Tenki.service.WidgetUpdateService;

import java.util.Date;

public class SettingsActivity extends Activity {

    ActivitySettingViewBinding mBinding;
    Prefs                      mPrefs;
    WeatherIconManager         mIconManager;
    YahooWeather               mPreviewData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivitySettingViewBinding.setContentView(this);

        mPrefs = TenkiApp.from(this).getPrefs();
        mPreviewData = getDummyWeatherData();

        mBinding.theme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updatePreview();
            }
        });
        mBinding.theme.check(mPrefs.get(Prefs.WW_THEME) == WidgetTheme.THEME_DARK
                ? R.id.darkTheme : R.id.theme1);
        mIconManager = new WeatherIconManager(this);

        updatePreview();
    }

    @Override
    protected void onPause() {
        super.onPause();

        int theme = getSelectedTheme();
        mPrefs.set(Prefs.WW_THEME, theme);

        startService(new Intent(this, WidgetUpdateService.class));
    }

    public int getSelectedTheme() {
        return (mBinding.theme.getCheckedRadioButtonId() == R.id.darkTheme)
                ? WidgetTheme.THEME_DARK : WidgetTheme.THEME_LIGHT;
    }

    YahooWeather getDummyWeatherData() {
        YahooWeather yw = new YahooWeather();
        yw.areaName = "地域名";
        yw.today = new YahooWeather.Day();
        yw.tomorrow = new YahooWeather.Day();
        yw.days = new YahooWeather.WeeklyDay[8];

        Date time = new Date();
        yw.today.date = time;
        yw.today.hours = new YahooWeather.Hour[8];
        for (int j = 0; j < yw.today.hours.length; j++) {
            yw.today.hours[j] = getDummyHourData(j * 3);
        }

        time = new Date(time.getTime() + DateUtils.DAY_IN_MILLIS);
        yw.tomorrow.date = time;
        yw.tomorrow.hours = new YahooWeather.Hour[8];
        for (int j = 0; j < yw.tomorrow.hours.length; j++) {
            yw.tomorrow.hours[j] = getDummyHourData(j * 3);
        }

        return yw;
    }


    YahooWeather.Hour getDummyHourData(int hourOfDay) {
        YahooWeather.Hour hour = new YahooWeather.Hour();
        hour.hour = hourOfDay;
        hour.temp = "10";
        hour.rain = "0";
        hour.setImageUrl("psun.gif");
        return hour;
    }


    void updatePreview() {
        WidgetTheme theme = new WidgetTheme(getSelectedTheme());
        RemoteViews rv    = WidgetUpdateService.buildUpdate(this, mPreviewData, theme, true, mIconManager);
        View        v     = rv.apply(this, mBinding.widgetPreview);
        mBinding.widgetPreview.removeAllViews();
        mBinding.widgetPreview.addView(v);
    }
}
