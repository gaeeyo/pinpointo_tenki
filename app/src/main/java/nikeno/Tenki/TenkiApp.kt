package nikeno.Tenki

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TenkiApp : Application() {
    val prefs: Prefs by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TenkiApp)
            modules(tenkiAppModule)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nc = NotificationChannel(
                N_CH_WIDGET_UPDATE_SERVICE,
                getString(R.string.widgetUpdateService),
                NotificationManager.IMPORTANCE_NONE
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(nc)
        }
    }

    companion object {
        const val IMAGE_SIZE_MAX: Int = 16 * 1024
        const val HTML_SIZE_MAX: Int = 50 * 1024

        /**
         * リロードせずにキャッシュを利用する時間
         */
        const val PRIORITY_CACHE_TIME: Long = 10 * DateUtils.MINUTE_IN_MILLIS
        const val CONNECT_TIMEOUT: Int = (10 * DateUtils.SECOND_IN_MILLIS).toInt()

        const val N_ID_WIDGET_UPDATE_SERVICE: Int = 1

        const val N_CH_WIDGET_UPDATE_SERVICE: String = "widgetUpdateService"

        @JvmStatic
        fun from(context: Context): TenkiApp {
            return context.applicationContext as TenkiApp
        }
    }
}

