package nikeno.Tenki;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;

public class Const {
	public static final int IMAGE_SIZE_MAX = 16*1024;
	public static final int HTML_SIZE_MAX = 50*1024;

	/** リロードせずにキャッシュを利用する時間 */
	public static final long PRIORITY_CACHE_TIME = 10 * DateUtils.MINUTE_IN_MILLIS;

	public static final int CONNECT_TIMEOUT = (int)(10 * DateUtils.SECOND_IN_MILLIS);

	public static boolean DEBUG = true;

	public static float DENSITY = 1f;
	public static int DENSITY_DPI = 160;

	public static void init(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		DENSITY = dm.density;
		DENSITY_DPI = dm.densityDpi;
	}
}
