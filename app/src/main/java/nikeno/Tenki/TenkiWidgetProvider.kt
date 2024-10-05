package nikeno.Tenki

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs
import nikeno.Tenki.appwidget.weatherwidget.clearWork
import nikeno.Tenki.appwidget.weatherwidget.setupWork

private const val TAG = "TenkiWidgetProvider"

class TenkiWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d(TAG, "onReceive action=${intent?.action}")
        if (context != null && intent?.action == ACTION_UPDATE) {
            Log.d(TAG, "更新ボタンによる更新")
            setupWork(context)
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled")
        setupWork(context!!)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled")
        clearWork(context!!)
    }

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate ids=[${appWidgetIds.joinToString(",")}]")
//        updateWidgetOnce(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "onDeleted ids=[${appWidgetIds.joinToString(",")}]")
        WeatherWidgetPrefs.deleteWidgetConfig(context, appWidgetIds)
    }

    companion object {
        const val ACTION_UPDATE = "nikeno.Tenki.action.update"
    }
}
