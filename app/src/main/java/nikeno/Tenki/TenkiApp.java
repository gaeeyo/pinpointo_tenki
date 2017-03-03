package nikeno.Tenki;

import android.app.Application;
import android.text.format.DateUtils;

public class TenkiApp extends Application {
	public static final int IMAGE_SIZE_MAX = 16*1024;
	public static final int HTML_SIZE_MAX = 50*1024;
	/** リロードせずにキャッシュを利用する時間 */
	public static final long PRIORITY_CACHE_TIME = 10 * DateUtils.MINUTE_IN_MILLIS;
	public static final int CONNECT_TIMEOUT = (int)(10 * DateUtils.SECOND_IN_MILLIS);
}
