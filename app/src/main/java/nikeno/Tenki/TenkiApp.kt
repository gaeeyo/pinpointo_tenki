package nikeno.Tenki

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.format.DateUtils

class TenkiApp : Application() {
    private lateinit var mPrefs: Prefs
    private var mDownloader: Downloader? = null

    override fun onCreate() {
        super.onCreate()

        mPrefs = Prefs(getSharedPreferences("AF.Tenki", MODE_PRIVATE))

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

    val prefs: Prefs
        get() = mPrefs

    @get:Synchronized
    val downloader: Downloader
        get() {
            if (mDownloader == null) {
                mDownloader = Downloader(this, DEFAULT_CACHE_FILENAME)
            }
            return mDownloader!!
        }

    companion object {
        private const val DEFAULT_CACHE_FILENAME = "file_cache.db"

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

val Context.prefs
    get() = (this.applicationContext as TenkiApp).prefs

