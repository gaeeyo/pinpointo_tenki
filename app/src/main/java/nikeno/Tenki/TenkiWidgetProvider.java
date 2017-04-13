package nikeno.Tenki;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.SparseArray;

import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.service.MyJobService;
import nikeno.Tenki.service.WidgetUpdateService;

public class TenkiWidgetProvider extends AppWidgetProvider {

	private static final String EXTRA_WAKE_LOCK_ID = "android.support.content.wakelockid";

	static final String TAG = TenkiWidgetProvider.class.getSimpleName();

	private static final SparseArray<PowerManager.WakeLock> mActiveWakeLocks
			= new SparseArray<PowerManager.WakeLock>();
	private static int mNextId = 1;

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		if (intent != null && intent.getAction() != null) {
			switch (intent.getAction()) {
				case Intent.ACTION_MY_PACKAGE_REPLACED:
					context.startService(new Intent(context, WidgetUpdateService.class));
					break;
			}
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		ScheduleCompat.getInstance().setup(context);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		TenkiWidgetConfigure.deleteWidgetConfig(context, appWidgetIds);
	}

	abstract static class ScheduleCompat {


		static ScheduleCompat sInstance;

		static ScheduleCompat getInstance() {
			if (sInstance == null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					sInstance =  new ScheduleCompat_Lolipop();
				} else {
					sInstance = new ScheduleCompat_v4();
				}
			}
			return sInstance;
		}

		abstract void setup(Context context);
	}

	static class ScheduleCompat_v4 extends ScheduleCompat {
		@Override
		public void setup(Context context) {
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			Intent intent = new Intent(context, WidgetUpdateService.class);
			PendingIntent pi = PendingIntent.getService(context, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS,
					AlarmManager.INTERVAL_HOUR, pi);

			context.startService(intent);
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	static class ScheduleCompat_Lolipop extends ScheduleCompat {
		@Override
		public void setup(Context context) {

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WidgetUpdateService.class);
            PendingIntent pi = PendingIntent.getService(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);

			JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
			JobInfo job = new JobInfo.Builder(1, new ComponentName(context, MyJobService.class))
					.setPeriodic(30 * DateUtils.MINUTE_IN_MILLIS)
					.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
					.build();
			js.schedule(job);
		}
	}
}
