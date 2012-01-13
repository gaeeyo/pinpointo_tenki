package nikeno.Tenki.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

import nikeno.Tenki.Const;
import nikeno.Tenki.Downloader;
import nikeno.Tenki.MainActivity;
import nikeno.Tenki.R;
import nikeno.Tenki.TenkiWidgetProvider;
import nikeno.Tenki.YahooWeather;
import nikeno.Tenki.YahooWeather.Day;
import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.activity.TenkiWidgetConfigure.WidgetConfig;

public class WidgetUpdateService extends Service {

	static final String TAG = WidgetUpdateService.class.getSimpleName();

	UpdateTask mUpdateTask;


	public static void start(Context context) {
		context.startService(new Intent(context, WidgetUpdateService.class));
	}

	/**
	 * デフォルトの間隔でアラームを設定
	 * @param context
	 */
	public static void setAlarm(Context context) {
		setAlarm(context, 1*DateUtils.HOUR_IN_MILLIS);
	}

	/**
	 * アラームを設定
	 * @param context
	 * @param interval
	 */
	public static void setAlarm(Context context, long interval) {
		if (Const.DEBUG) Log.d(TAG, "setAlarm");

		AlarmManager am = (AlarmManager)context.getSystemService(ALARM_SERVICE);

		Intent intent = new Intent(context, WidgetUpdateService.class);
		PendingIntent pi = PendingIntent.getService(context,
				0, intent, 0);

		am.setRepeating(AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + interval,
				interval, pi);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (Const.DEBUG) {
			Log.d(TAG, "UpdateService started");
		}
		super.onStart(intent, startId);

		startUpdateTask();
	}

	void onUpdateTaskFinished() {
		mUpdateTask = null;
		stopSelf();
	}

	private void startUpdateTask() {
		if (mUpdateTask == null) {
			mUpdateTask = new UpdateTask() {
				@Override
				protected void onPostExecute(Object result) {
					super.onPostExecute(result);
					onUpdateTaskFinished();
				}
				@Override
				protected void onCancelled() {
					super.onCancelled();
					onUpdateTaskFinished();
				}
			};
			mUpdateTask.execute(this);
		}
	}

	// -----------------------------------
	// UpdateTask
	// -----------------------------------

	private static class UpdateTask extends AsyncTask<Object, Integer, Object> {

		@Override
		protected Object doInBackground(Object... params) {
			Context context = (Context)params[0];
			updateWidgets(context);
			return null;
		}

		public static void updateWidgets(Context context) {
			ComponentName thisWidget = new ComponentName(context, TenkiWidgetProvider.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			int [] ids = manager.getAppWidgetIds(thisWidget);

			WidgetTheme theme = new WidgetTheme(context);

			Log.d(TAG, "updateWidgets count:"+ids.length);
			for (int id: ids) {
				WidgetConfig config = TenkiWidgetConfigure.getWidgetConfig(context, id);

				try {
					byte [] html = Downloader.download(config.url, Const.HTML_SIZE_MAX,
							10*DateUtils.SECOND_IN_MILLIS, true);
					YahooWeather data = YahooWeather.parse(html);

					RemoteViews views = buildUpdate(context, id, data, theme);

					Intent i = new Intent(context, MainActivity.class);
					i.setData(Uri.parse(config.url));
					PendingIntent pi = PendingIntent.getActivity(context, 0,
							i, 0);
					views.setOnClickPendingIntent(R.id.container, pi);
					manager.updateAppWidget(id, views);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		public static RemoteViews buildUpdate(Context context, int id, YahooWeather data,
				WidgetTheme theme) {
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget);

			String time = DateUtils.formatDateTime(context, System.currentTimeMillis(),
					DateUtils.FORMAT_SHOW_TIME);
			views.setTextViewText(R.id.title,data.areaName);
			views.setTextViewText(R.id.time, time + " 更新");

			int [] heads = new int [] {
					R.id.h0, R.id.h1, R.id.h2, R.id.h3, R.id.h4,
					R.id.h5, R.id.h6, R.id.h7
				};
			int [] cols = new int [] {
					R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4,
					R.id.t5, R.id.t6, R.id.t7
				};
			int [] imgs = new int [] {
					R.id.i0, R.id.i1, R.id.i2, R.id.i3, R.id.i4,
					R.id.i5, R.id.i6, R.id.i7
				};

			final int HOUR = 60 * 60 * 1000;
			Calendar nowJapan = Calendar.getInstance(Locale.JAPAN);
			long now = nowJapan.getTime().getTime() - 3 * HOUR;
			int col = 0;

			for (int y=0; y<2; y++) {
				Day day = (y == 0 ? data.today : data.tomorrow);

				long baseTime = day.date.getTime();

				for (int x=0; x<8; x++) {
					YahooWeather.Hour h = day.hours[x];

					boolean enabled = (baseTime + h.hour * HOUR) > now;
					if (enabled && col < cols.length) {

						views.setTextViewText(heads[col],
								String.valueOf(h.hour) +"時");

						StringBuilder sb = new StringBuilder();

						sb.append(h.text + "\n");
						int textEnd = sb.length();

						sb.append(h.temp + "℃\n");
						int tempEnd = sb.length();

						//sb.append(h.humidity + "\n");
						int humidEnd = sb.length();

						sb.append(h.rain + "㍉");
						int rainEnd = sb.length();

						String imageUrl = h.getImageUrl(true);
						Log.d(TAG, "imagerUrl:"+imageUrl);
						if (imageUrl != null) {
							int bmpId = getBitmap(context, imageUrl);;
							if (bmpId != -1) {
								views.setImageViewResource(imgs[col], bmpId);
							}
							else {
								Bitmap bmp = null;
								try {
									bmp = Downloader.downloadImage(imageUrl, 8000, 0);
									views.setImageViewBitmap(imgs[col], bmp);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}

						SpannableString ss = new SpannableString(sb);
						ss.setSpan(new ForegroundColorSpan(theme.textColor), 0, textEnd,
								SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
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

	}

	private static final String [] bmpNames = new String [] {
		"psun", "psnow", "psleet",
		"prain", "pmoon", "pclouds"
	};
	private static final int [] bmpIds = new int [] {
		R.drawable.psun, R.drawable.psnow, R.drawable.psleet,
		R.drawable.prain, R.drawable.pmoon, R.drawable.pclouds
	};

	public static int getBitmap(Context c, String url) {
		for (int j=0; j<bmpNames.length; j++) {
			if (url.indexOf(bmpNames[j]) != -1) {
				return bmpIds[j];
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
