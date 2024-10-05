package nikeno.Tenki.activity

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs
import nikeno.Tenki.appwidget.weatherwidget.setupWork
import nikeno.Tenki.ui.app.MyAppTheme
import nikeno.Tenki.ui.screen.selectarea.SelectAreaScreen

class TenkiWidgetConfigure : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == null || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyAppTheme {
                SelectAreaScreen(onSelectArea = {
                    setWidgetAreaUrl(widgetId, it.url)
                    setResult(RESULT_OK)
                    finish()
                }, onBackPressed = {
                    finish()
                })
            }
        }
    }

    private fun setWidgetAreaUrl(id: Int, url: String) {
        WeatherWidgetPrefs.addWidgetConfig(this, id, url)
        setupWork(this)
    }
}
