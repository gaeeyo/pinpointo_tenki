package nikeno.Tenki.util;

import android.app.PendingIntent;
import android.os.Build;

public class PendingIntentCompat {
    public static final int FLAG_MUTABLE = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
}
