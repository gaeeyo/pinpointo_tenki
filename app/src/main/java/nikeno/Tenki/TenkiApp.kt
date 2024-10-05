package nikeno.Tenki

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TenkiApp : Application() {
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
        const val N_ID_WIDGET_UPDATE_SERVICE: Int = 1
        const val N_CH_WIDGET_UPDATE_SERVICE: String = "widgetUpdateService"
    }
}

