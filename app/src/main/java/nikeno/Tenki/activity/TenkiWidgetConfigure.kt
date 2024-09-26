package nikeno.Tenki.activity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import nikeno.Tenki.activity.AreaSelectActivity
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs
import nikeno.Tenki.service.WidgetUpdateService

class TenkiWidgetConfigure : Activity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                mAppWidgetId
            )
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val i = Intent(this, AreaSelectActivity::class.java)
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                if (data != null) {
                    // ウィジェットの設定を更新 
                    val url = data.getStringExtra("url")
                    WeatherWidgetPrefs.addWidgetConfig(this, mAppWidgetId, url)


                    // Homeアプリに戻り値を返す
                    val i = Intent()
                    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                    setResult(RESULT_OK, i)


                    // ウィジェットを更新する
                    startService(Intent(this, WidgetUpdateService::class.java))
                }
                finish()
            }

            else -> finish()
        }
    }

    companion object {
        private val TAG: String = TenkiWidgetConfigure::class.java.simpleName
    }
}
