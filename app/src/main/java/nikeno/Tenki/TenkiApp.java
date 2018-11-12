package nikeno.Tenki;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.text.format.DateUtils;

public class TenkiApp extends Application {
	public static final int IMAGE_SIZE_MAX = 16*1024;
	public static final int HTML_SIZE_MAX = 50*1024;
	/** リロードせずにキャッシュを利用する時間 */
	public static final long PRIORITY_CACHE_TIME = 10 * DateUtils.MINUTE_IN_MILLIS;
	public static final int CONNECT_TIMEOUT = (int)(10 * DateUtils.SECOND_IN_MILLIS);

	public static final int N_ID_WIDGET_UPDATE_SERVICE = 1;

	public static final String N_CH_WIDGET_UPDATE_SERVICE = "widgetUpdateService";

	@Override
	public void onCreate() {
		super.onCreate();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel nc = new NotificationChannel(TenkiApp.N_CH_WIDGET_UPDATE_SERVICE, getString(R.string.widgetUpdateService), NotificationManager.IMPORTANCE_NONE);
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.createNotificationChannel(nc);
		}
	}
}
