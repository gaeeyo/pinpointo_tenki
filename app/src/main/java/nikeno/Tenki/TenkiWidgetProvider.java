package nikeno.Tenki;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.SparseArray;

import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.service.WidgetUpdateService;

public class TenkiWidgetProvider extends AppWidgetProvider {

	private static final String EXTRA_WAKE_LOCK_ID = "android.support.content.wakelockid";

	static final String TAG = TenkiWidgetProvider.class.getSimpleName();

	private static final SparseArray<PowerManager.WakeLock> mActiveWakeLocks
			= new SparseArray<PowerManager.WakeLock>();
	private static int mNextId = 1;


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);


        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, WidgetUpdateService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS,
                AlarmManager.INTERVAL_HOUR, pi);

		context.startService(intent);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		TenkiWidgetConfigure.deleteWidgetConfig(context, appWidgetIds);
	}

	public static ComponentName startWakefulService(Context context, Intent intent) {
		synchronized (mActiveWakeLocks) {
			int id = mNextId;
			mNextId++;
			if (mNextId <= 0) {
				mNextId = 1;
			}

			intent.putExtra(EXTRA_WAKE_LOCK_ID, id);
			ComponentName comp = context.startService(intent);
			if (comp == null) {
				return null;
			}

			PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					"wake:" + comp.flattenToShortString());
			wl.setReferenceCounted(false);
			wl.acquire(60*1000);
			mActiveWakeLocks.put(id, wl);
			return comp;
		}
	}

	public static boolean completeWakefulIntent(Intent intent) {
		final int id = intent.getIntExtra(EXTRA_WAKE_LOCK_ID, 0);
		if (id == 0) {
			return false;
		}
		synchronized (mActiveWakeLocks) {
			PowerManager.WakeLock wl = mActiveWakeLocks.get(id);
			if (wl != null) {
				wl.release();
				mActiveWakeLocks.remove(id);
				return true;
			}
			// We return true whether or not we actually found the wake lock
			// the return code is defined to indicate whether the Intent contained
			// an identifier for a wake lock that it was supposed to match.
			// We just log a warning here if there is no wake lock found, which could
			// happen for example if this function is called twice on the same
			// intent or the process is killed and restarted before processing the intent.
//			Log.w("WakefulBroadcastReceiver", "No active wake lock id #" + id);
			return true;
		}
	}
}
