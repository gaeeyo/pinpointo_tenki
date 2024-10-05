package nikeno.Tenki.appwidget.weatherwidget

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

object WeatherWidgetPrefs {
    const val TAG: String = "WidgetSettings"

    const val NAME_URL: String = "name"

    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun prefKey(id: Int, name: String): String {
        return "widget_$id$name"
    }

    fun addWidgetConfig(context: Context, id: Int, url: String?) {
        getPrefs(context).edit().putString(prefKey(id, NAME_URL), url).apply()
    }

    fun getWidgetConfig(context: Context, id: Int): WeatherWidgetConfig {
        val url = getPrefs(context).getString(prefKey(id, NAME_URL), "")
        return WeatherWidgetConfig(url)
    }

    fun deleteWidgetConfig(context: Context, ids: IntArray) {
        val edit = getPrefs(context).edit()

        for (id in ids) {
            Log.d(TAG, "deleteWidgetConfig id:$id")
            edit.remove(prefKey(id, "url"))
        }
        edit.apply()
    }
}
