package nikeno.Tenki.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;
import nikeno.Tenki.*;
import nikeno.Tenki.YahooWeather.Day;
import nikeno.Tenki.appwidget.weatherwidget.WeatherIconManager;
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetConfig;
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs;
import nikeno.Tenki.appwidget.weatherwidget.WidgetTheme;
import nikeno.Tenki.db.entity.ResourceCacheEntity;
import nikeno.Tenki.util.PendingIntentCompat;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;

import static nikeno.Tenki.TenkiApp.N_ID_WIDGET_UPDATE_SERVICE;

public class WidgetUpdateService extends IntentService {

    static final String TAG                  = WidgetUpdateService.class.getSimpleName();
    static final String ACTION_MANUAL_UPDATE = "manualUpdate";

    public WidgetUpdateService() {
        super("WidgetUpdateService");
    }

    static PendingIntent getManualReloadPendingIntent(Context context) {
        Intent i = new Intent(context, WidgetUpdateService.class);
        i.setAction(ACTION_MANUAL_UPDATE);
        return PendingIntent.getService(context, 0, i,
                PendingIntentCompat.FLAG_MUTABLE);
    }

    public static RemoteViews buildUpdate(Context context, YahooWeather data,
                                          WidgetTheme theme, boolean forceCache, WeatherIconManager iconManager) {

        int layout;
        switch (theme.theme) {
            case WidgetTheme.THEME_LIGHT:
                layout = R.layout.weather_widget_light;
                break;
            case WidgetTheme.THEME_DARK:
            default:
                layout = R.layout.weather_widget_dark;
                break;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(),
                layout);

        String time = DateUtils.formatDateTime(context, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
        views.setTextViewText(R.id.title, data.areaName);
        if (forceCache) {
            views.setTextViewText(R.id.time, context.getString(R.string.updateErrorTimeFmt, time));
        } else {
            views.setTextViewText(R.id.time, context.getString(R.string.updateTimeFmt, time));
        }
        views.setOnClickPendingIntent(R.id.time, getManualReloadPendingIntent(context));
        views.setOnClickPendingIntent(R.id.h6, getManualReloadPendingIntent(context));
        views.setOnClickPendingIntent(R.id.h7, getManualReloadPendingIntent(context));

        int[] heads = new int[]{
                R.id.h0, R.id.h1, R.id.h2, R.id.h3, R.id.h4,
                R.id.h5, R.id.h6, R.id.h7
        };
        int[] cols = new int[]{
                R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4,
                R.id.t5, R.id.t6, R.id.t7
        };
        int[] imgs = new int[]{
                R.id.i0, R.id.i1, R.id.i2, R.id.i3, R.id.i4,
                R.id.i5, R.id.i6, R.id.i7
        };

        final int HOUR     = 60 * 60 * 1000;
        Calendar  nowJapan = Calendar.getInstance(Locale.JAPAN);
        long      now      = nowJapan.getTime().getTime() - 3 * HOUR;
        int       col      = 0;

        for (int y = 0; y < 2; y++) {
            Day day = (y == 0 ? data.today : data.tomorrow);

            long baseTime = day.date.getTime();

            for (int x = 0; x < 8; x++) {
                YahooWeather.Hour h = day.hours[x];

                boolean enabled = (baseTime + h.hour * HOUR) > now;
                if (enabled && col < cols.length) {

                    views.setTextViewText(heads[col],
                            String.valueOf(h.hour) + "時");

                    StringBuilder sb = new StringBuilder();

//                        sb.append(h.text).append("\n");
                    int textEnd = sb.length();

                    sb.append(h.temp).append("℃\n");
                    int tempEnd = sb.length();

                    //sb.append(h.humidity + "\n");
                    int humidEnd = sb.length();

                    sb.append(h.rain).append("㍉");
                    int rainEnd = sb.length();

                    String imageUrl = h.getImageUrl(true);
                    Log.d(TAG, "imagerUrl:" + imageUrl);
                    if (imageUrl != null) {
                        int bmpId = getBitmapIndexFromUrl(context, imageUrl);
                        if (bmpId != -1) {
                            views.setImageViewResource(imgs[col], bmpId);
                        } else {
                            Bitmap bmp = iconManager.getIcon(imageUrl);
                            views.setImageViewBitmap(imgs[col], bmp);
                        }
                    }

                    SpannableString ss = new SpannableString(sb);
//                        ss.setSpan(new ForegroundColorSpan(theme.textColor), 0, textEnd,
//                                SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(theme.tempColor), textEnd, tempEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(theme.humidColor), tempEnd, humidEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(theme.rainColor), humidEnd, rainEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                    views.setTextViewText(cols[col], ss);

                    col++;
                }
            }
        }
        return views;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent " + intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "startForeground");
            Notification n = new Notification.Builder(this, TenkiApp.N_CH_WIDGET_UPDATE_SERVICE)
                    .setOngoing(true)
                    .build();
            startForeground(N_ID_WIDGET_UPDATE_SERVICE, n);
        }

        Log.d(TAG, "ウィジェット更新中");
        boolean isManualUpdate = (intent != null && ACTION_MANUAL_UPDATE.equals(intent.getAction()));
        new UpdateTask().updateWidgets(this, isManualUpdate);
        Log.d(TAG, "ウィジェット更新完了");
    }

    static class UpdateTask {

        void updateWidgets(Context context, boolean isManualUpdate) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, TenkiWidgetProvider.class));

            WidgetTheme theme = new WidgetTheme(
                    TenkiApp.from(context).getPrefs().get(Prefs.WW_THEME));

            if (isManualUpdate) {
                for (int id : ids) {
                    manager.updateAppWidget(id, createProgressView(context));
                }
            }

            Log.d(TAG, "updateWidgets count:" + ids.length);
            Throwable error = null;
            for (int id : ids) {
                for (int retryCount = 0; retryCount < 1; retryCount++) {
                    try {
//                        if (retryCount > 0) {
//                            Thread.sleep((retryCount - 1) * 3000);
//                        }
                        updateWidget(context, manager, id, theme, false);
                        error = null;
                        break;
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        error = e;
                    } catch (Exception e) {
                        e.printStackTrace();
                        error = e;
                        break;
                    }
                }
                if (error != null) {
                    try {
                        Log.d(TAG, "キッシュで更新");
                        updateWidget(context, manager, id, theme, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        manager.updateAppWidget(id, createErrorView(context, error));
                    }
                }
            }
        }

        void updateWidget(Context context, AppWidgetManager manager,
                          int id, WidgetTheme theme, boolean forceCache) throws Exception {
            WeatherWidgetConfig config = WeatherWidgetPrefs.getWidgetConfig(context, id);

            final Downloader downloader = TenkiApp.from(context).getDownloader();
            byte[]           html;
            if (forceCache) {
                ResourceCacheEntity entry = downloader.getCache(config.url, 0);
                if (entry == null) {
                    throw new IOException("データの取得エラー。タップして再読み込み");
                }
                html = entry.data;
            } else {
                html = downloader.download(config.url, TenkiApp.HTML_SIZE_MAX,
                        System.currentTimeMillis() - 15 * DateUtils.MINUTE_IN_MILLIS, true);
            }
            YahooWeather data = YahooWeather.parse(html);

            RemoteViews views = buildUpdate(context, data, theme, forceCache, getIconManager(context));

            Intent i = new Intent(context, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(Uri.parse(config.url));
            PendingIntent pi = PendingIntent.getActivity(context, 0,
                    i, PendingIntentCompat.FLAG_MUTABLE);
            views.setOnClickPendingIntent(R.id.container, pi);
            manager.updateAppWidget(id, views);
        }

        RemoteViews createErrorView(Context context, Throwable e) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_error);
            String time = DateUtils.formatDateTime(context, System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
            views.setTextViewText(R.id.errorMessage, time + ":" + e.getMessage());
            views.setOnClickPendingIntent(R.id.errorMessage, getManualReloadPendingIntent(context));
            return views;
        }

        RemoteViews createProgressView(Context context) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_progress);
            views.setOnClickPendingIntent(R.id.progress, getManualReloadPendingIntent(context));
            return views;
        }

        WeatherIconManager mIconManager;

        WeatherIconManager getIconManager(Context context) {
            if (mIconManager == null) {
                mIconManager = new WeatherIconManager(context);
            }
            return mIconManager;
        }
    }


    private static final String[] bmpNames = new String[]{
            "psun", "psnow", "psleet",
            "prain_light", "prain_gusty",
            "prain", "pmoon", "pclouds"
    };

    private static final int[] bmpIds = new int[]{
            R.drawable.psun, R.drawable.psnow, R.drawable.psleet,
            R.drawable.prain, R.drawable.prain_gusty,
            R.drawable.prain, R.drawable.pmoon, R.drawable.pclouds
    };

    public static int getBitmapIndexFromUrl(Context c, String url) {
        return getBitmapIndexFromUrl_old(c, url);
    }

    public static int getBitmapIndexFromUrl_old(Context c, String url) {
        if (url != null) {
            for (int j = 0; j < bmpNames.length; j++) {
                if (url.contains(bmpNames[j])) {
                    return bmpIds[j];
                }
            }
        }
        return -1;
    }

}
