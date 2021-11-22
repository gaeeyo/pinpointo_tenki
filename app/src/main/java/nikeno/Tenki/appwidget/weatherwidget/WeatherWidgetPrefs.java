package nikeno.Tenki.appwidget.weatherwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import nikeno.Tenki.Utils;

public class WeatherWidgetPrefs {
    static final String TAG = "WidgetSettings";

    static final String NAME_URL = "name";

    @NonNull
    private static SharedPreferences getPrefs(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    private static String prefKey(int id, @NonNull String name) {
        return "widget_" + id + name;
    }

    public static void addWidgetConfig(@NonNull Context context, int id, String url) {
        getPrefs(context).edit().putString(prefKey(id, NAME_URL), url).apply();
    }

    @NonNull
    public static WeatherWidgetConfig getWidgetConfig(@NonNull Context context, int id) {
        String url = Utils.httpsUrl(getPrefs(context).getString(prefKey(id, NAME_URL), ""));;
        return new WeatherWidgetConfig(url);
    }

    public static void deleteWidgetConfig(@NonNull Context context, @NonNull int[] ids) {
        SharedPreferences.Editor edit = getPrefs(context).edit();

        for (int id : ids) {
            Log.d(TAG, "deleteWidgetConfig id:" + id);
            edit.remove(prefKey(id, "url"));
        }
        edit.apply();
    }
}
