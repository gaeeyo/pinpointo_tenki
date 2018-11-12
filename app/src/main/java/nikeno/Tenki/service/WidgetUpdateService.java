package nikeno.Tenki.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import nikeno.Tenki.Downloader;
import nikeno.Tenki.MainActivity;
import nikeno.Tenki.R;
import nikeno.Tenki.TenkiApp;
import nikeno.Tenki.TenkiWidgetProvider;
import nikeno.Tenki.YahooWeather;
import nikeno.Tenki.YahooWeather.Day;
import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.activity.TenkiWidgetConfigure.WidgetConfig;

import static nikeno.Tenki.TenkiApp.N_ID_WIDGET_UPDATE_SERVICE;

public class WidgetUpdateService extends IntentService {

    static final String TAG = WidgetUpdateService.class.getSimpleName();

    UpdateTask mUpdateTask;

    public WidgetUpdateService() {
        super("WidgetUpdateService");
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

        if (mUpdateTask != null) return;

//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
//                getPackageName() + ":" + getClass().getName());
//        mWakeLock.setReferenceCounted(false);
//        mWakeLock.acquire(60*1000);

        mUpdateTask = new UpdateTask() {
            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                onFinishUpdateTask();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                onFinishUpdateTask();
            }
        };
        mUpdateTask.execute(this);
    }

    void onFinishUpdateTask() {
        mUpdateTask = null;
        stopSelf();
    }

    public static class UpdateTask extends AsyncTask<Object, Integer, Object> {

        @Override
        protected Object doInBackground(Object... params) {
            Context context = (Context) params[0];

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, TenkiWidgetProvider.class));

            WidgetTheme theme = new WidgetTheme(context);

            Log.d(TAG, "updateWidgets count:" + ids.length);
            Throwable error = null;
            for (int id : ids) {
                for (int retryCount = 0; retryCount < 3; retryCount++) {
                    try {
                        if (retryCount > 0) {
                            Thread.sleep((retryCount - 1) * 3000);
                        }
                        updateWidget(context, manager, id, theme);
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
//                    manager.updateAppWidget(id, createErrorView(context, error));
                }
            }
            return null;
        }

        void updateWidget(Context context, AppWidgetManager manager,
                          int id, WidgetTheme theme) throws Exception {
            WidgetConfig config = TenkiWidgetConfigure.getWidgetConfig(context, id);

            byte[] html = Downloader.getInstance(context).download(config.url, TenkiApp.HTML_SIZE_MAX,
                    System.currentTimeMillis() - 15 * DateUtils.MINUTE_IN_MILLIS, true);
            YahooWeather data = YahooWeather.parse(html);

            RemoteViews views = buildUpdate(context, id, data, theme);

            Intent i = new Intent(context, MainActivity.class);
            i.setData(Uri.parse(config.url));
            PendingIntent pi = PendingIntent.getActivity(context, 0,
                    i, 0);
            views.setOnClickPendingIntent(R.id.container, pi);
            manager.updateAppWidget(id, views);
        }


        RemoteViews createErrorView(Context context, Throwable e) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_error);
            String time = DateUtils.formatDateTime(context, System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
            views.setTextViewText(R.id.errorMessage, time + ":" + e.getMessage());
            return views;
        }

        RemoteViews buildUpdate(Context context, int id, YahooWeather data,
                                WidgetTheme theme) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget);

            String time = DateUtils.formatDateTime(context, System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
            views.setTextViewText(R.id.title, data.areaName);
            views.setTextViewText(R.id.time, time + " 更新");

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
                                Bitmap bmp = getIconManager(context).getIcon(imageUrl);
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

        WeatherIconManager mIconManager;

        WeatherIconManager getIconManager(Context context) {
            if (mIconManager == null) {
                mIconManager = new WeatherIconManager(context);
            }
            return mIconManager;
        }
    }



    static class WeatherIconManager {

        private final Context                mContext;
        private       HashMap<String,Bitmap> mCache = new HashMap<>();
        int mIconSize;
        int mShadowOffset;

        public WeatherIconManager(Context context) {
            mContext = context;
            float density = context.getResources().getDisplayMetrics().density;
            mShadowOffset = Math.round(1 * density);
            mIconSize = (int) (38 * density) + mShadowOffset * 2;
        }

        public Bitmap getIcon(String url) {
            try {
                Bitmap bmp = mCache.get(url);
                if (bmp != null) return bmp;

                bmp = Downloader.getInstance(mContext).downloadImage(url, 8000, 0);
                if (bmp != null) {
                    Bitmap newBitmap= convertBitmap(bmp);
                    if (newBitmap != null) {
                        bmp = newBitmap;
                    }
                    mCache.put(url, bmp);
                }
                return bmp;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public Bitmap convertBitmap(Bitmap bmp) {
            Bitmap newBmp = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
            Bitmap alpha = bmp.extractAlpha();
            try {
                Canvas c     = new Canvas(newBmp);
                Paint  p     = new Paint();
                p.setFilterBitmap(true);
                p.setColor(0x88000000);
                Matrix m      = new Matrix();
                float  scaleX = (float)(mIconSize - mShadowOffset*2) / bmp.getWidth();
                float  scaleY = (float)(mIconSize - mShadowOffset*2) / bmp.getHeight();
                float  scale  = Math.max(scaleX, scaleY);
                m.preScale(scale, scale);
                m.postTranslate(mShadowOffset*2, mShadowOffset*2);
                c.drawBitmap(alpha, m, p);
                m.postTranslate(-mShadowOffset, -mShadowOffset);
                c.drawBitmap(bmp, m, null);
            } finally {
                alpha.recycle();
            }

            return newBmp;
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

    public static class WidgetTheme {
        public final int timeColor;
        public final int tempColor;
        public final int rainColor;
        public final int humidColor;
        public final int textColor;

        public WidgetTheme(Context context) {
//			Resources res = context.getResources();

            timeColor = 0xffd0d0d0;
            tempColor = 0xffFFAC59;
            rainColor = 0xff8EC7FF;
            humidColor = 0xff8EFF8E;
            textColor = 0xffe0e0e0;
        }
    }
}
