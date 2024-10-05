package nikeno.Tenki.service

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import nikeno.Tenki.MainActivity
import nikeno.Tenki.R
import nikeno.Tenki.TenkiApp
import nikeno.Tenki.TenkiApp.Companion.from
import nikeno.Tenki.TenkiWidgetProvider
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs
import nikeno.Tenki.feature.fetcher.WeatherFetcher
import nikeno.Tenki.feature.weather.YahooWeather
import nikeno.Tenki.feature.weather.YahooWeather.Hour
import nikeno.Tenki.feature.weather.YahooWeatherHtmlParser
import nikeno.Tenki.util.PendingIntentCompat
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class WidgetUpdateServiceComponent : KoinComponent {
    val fetcher: WeatherFetcher by inject()
}

class WidgetUpdateService : IntentService("WidgetUpdateService") {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent $intent")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "startForeground")
            val n = Notification.Builder(this, TenkiApp.N_CH_WIDGET_UPDATE_SERVICE)
                .setOngoing(true)
                .build()
            startForeground(TenkiApp.N_ID_WIDGET_UPDATE_SERVICE, n)
        }

        Log.d(TAG, "ウィジェット更新中")
        val isManualUpdate = (intent != null && ACTION_MANUAL_UPDATE == intent.action)
        runBlocking {
            UpdateTask().updateWidgets(this@WidgetUpdateService, isManualUpdate)
        }
        Log.d(TAG, "ウィジェット更新完了")
    }

    internal class UpdateTask {

        val fetcher = WidgetUpdateServiceComponent().fetcher

        fun updateWidgets(context: Context, isManualUpdate: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TenkiWidgetProvider::class.java)
            )

            val theme = WidgetTheme(context)

            if (isManualUpdate) {
                for (id in ids) {
                    manager.updateAppWidget(id, createProgressView(context))
                }
            }

            Log.d(TAG, "updateWidgets count:" + ids.size)
            var error: Throwable? = null
            for (id in ids) {
                for (retryCount in 0..0) {
                    try {
//                        if (retryCount > 0) {
//                            Thread.sleep((retryCount - 1) * 3000);
//                        }
                        runBlocking { updateWidget(context, manager, id, theme, false) }
                        error = null
                        break
                    } catch (e: UnknownHostException) {
                        e.printStackTrace()
                        error = e
                    } catch (e: Exception) {
                        e.printStackTrace()
                        error = e
                        break
                    }
                }
                if (error != null) {
                    try {
                        Log.d(TAG, "キッシュで更新")
                        runBlocking { updateWidget(context, manager, id, theme, true) }
                    } catch (e: Exception) {
                        manager.updateAppWidget(id, createErrorView(context, error))
                    }
                }
            }
        }

        @Throws(Exception::class)
        suspend fun updateWidget(
            context: Context, manager: AppWidgetManager,
            id: Int, theme: WidgetTheme, forceCache: Boolean
        ) {
            val config = WeatherWidgetPrefs.getWidgetConfig(context, id)

            val weather = if (forceCache) {
                fetcher.getWeather(config.url, 0)
            } else {
                fetcher.getWeather(
                    config.url,
                    Clock.System.now().minus(15.minutes).toEpochMilliseconds()
                )
            }

            val views = when (weather) {
                is WeatherFetcher.WeatherResult.Error ->
                    createErrorView(context, weather.error)

                is WeatherFetcher.WeatherResult.Success ->
                    buildUpdate(context, id, weather.data, theme, forceCache)
            }

            val i = Intent(context, MainActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.setData(Uri.parse(config.url))
            val pi = PendingIntent.getActivity(
                context, 0,
                i, PendingIntentCompat.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.container, pi)
            manager.updateAppWidget(id, views)
        }

        fun getManualReloadPendingIntent(context: Context?): PendingIntent {
            val i = Intent(context, WidgetUpdateService::class.java)
            i.setAction(ACTION_MANUAL_UPDATE)
            return PendingIntent.getService(
                context, 0, i,
                PendingIntentCompat.FLAG_MUTABLE
            )
        }

        fun createErrorView(context: Context, e: Throwable): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_error
            )
            val time = DateUtils.formatDateTime(
                context, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
            )
            views.setTextViewText(R.id.errorMessage, time + ":" + e.message)
            views.setOnClickPendingIntent(R.id.errorMessage, getManualReloadPendingIntent(context))
            return views
        }

        fun createProgressView(context: Context): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_progress
            )
            views.setOnClickPendingIntent(R.id.progress, getManualReloadPendingIntent(context))
            return views
        }

        fun buildUpdate(
            context: Context, id: Int, data: YahooWeather,
            theme: WidgetTheme, forceCache: Boolean
        ): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                R.layout.widget
            )

            val time = DateUtils.formatDateTime(
                context, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
            )
            views.setTextViewText(R.id.title, data.areaName)
            if (forceCache) {
                views.setTextViewText(
                    R.id.time,
                    context.getString(R.string.updateErrorTimeFmt, time)
                )
            } else {
                views.setTextViewText(R.id.time, context.getString(R.string.updateTimeFmt, time))
            }
            views.setOnClickPendingIntent(R.id.time, getManualReloadPendingIntent(context))
            views.setOnClickPendingIntent(R.id.h6, getManualReloadPendingIntent(context))
            views.setOnClickPendingIntent(R.id.h7, getManualReloadPendingIntent(context))

            val heads = intArrayOf(
                R.id.h0, R.id.h1, R.id.h2, R.id.h3, R.id.h4,
                R.id.h5, R.id.h6, R.id.h7
            )
            val cols = intArrayOf(
                R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4,
                R.id.t5, R.id.t6, R.id.t7
            )
            val imgs = intArrayOf(
                R.id.i0, R.id.i1, R.id.i2, R.id.i3, R.id.i4,
                R.id.i5, R.id.i6, R.id.i7
            )

            val HOUR = 60 * 60 * 1000
            val nowJapan = Calendar.getInstance(Locale.JAPAN)
            val now = nowJapan.time.time - 3 * HOUR
            var col = 0

            for (y in 0..1) {
                val day: YahooWeather.Day = (if (y == 0) data.today else data.tomorrow)

                val baseTime: Long = day.date.toEpochMilliseconds()

                for (x in 0..7) {
                    val h: Hour = day.hours.get(x)

                    val enabled = (baseTime + h.hour * HOUR) > now
                    if (enabled && col < cols.size) {
                        views.setTextViewText(
                            heads[col],
                            h.hour.toString() + "時"
                        )

                        val sb = StringBuilder()

                        //                        sb.append(h.text).append("\n");
                        val textEnd = sb.length

                        sb.append(h.temp).append("℃\n")
                        val tempEnd = sb.length

                        //sb.append(h.humidity + "\n");
                        val humidEnd = sb.length

                        sb.append(h.rain).append("㍉")
                        val rainEnd = sb.length

                        val imageUrl = h.getImageUrl(true)
                        Log.d(TAG, "imagerUrl:$imageUrl")
                        if (imageUrl != null) {
                            val bmpId = getBitmapIndexFromUrl(context, imageUrl)
                            if (bmpId != -1) {
                                views.setImageViewResource(imgs[col], bmpId)
                            } else {
                                val bmp = getIconManager(context).getIcon(imageUrl)
                                views.setImageViewBitmap(imgs[col], bmp)
                            }
                        }

                        val ss = SpannableString(sb)
                        //                        ss.setSpan(new ForegroundColorSpan(theme.textColor), 0, textEnd,
//                                SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                        ss.setSpan(
                            ForegroundColorSpan(theme.tempColor), textEnd, tempEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        ss.setSpan(
                            ForegroundColorSpan(theme.humidColor), tempEnd, humidEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        ss.setSpan(
                            ForegroundColorSpan(theme.rainColor), humidEnd, rainEnd,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        views.setTextViewText(cols[col], ss)

                        col++
                    }
                }
            }
            return views
        }

        var mIconManager: WeatherIconManager? = null

        fun getIconManager(context: Context): WeatherIconManager {
            if (mIconManager == null) {
                mIconManager = WeatherIconManager(context)
            }
            return mIconManager!!
        }
    }


    internal class WeatherIconManager(private val mContext: Context) {
        val fetcher = WidgetUpdateServiceComponent().fetcher
        private val mCache = HashMap<String, Bitmap>()
        var mIconSize: Int
        var mShadowOffset: Int

        init {
            val density = mContext.resources.displayMetrics.density
            mShadowOffset = Math.round(1 * density)
            mIconSize = (38 * density).toInt() + mShadowOffset * 2
        }

        fun getIcon(url: String): Bitmap? {
            try {
                var bmp = mCache[url]
                if (bmp != null) return bmp

                bmp = runBlocking { fetcher.getImage2(url) }
                if (bmp != null) {
                    val newBitmap = convertBitmap(bmp)
                    if (newBitmap != null) {
                        bmp = newBitmap
                    }
                    mCache[url] = bmp
                }
                return bmp
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun convertBitmap(bmp: Bitmap): Bitmap {
            val newBmp = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888)
            val alpha = bmp.extractAlpha()
            try {
                val c = Canvas(newBmp)
                val p = Paint()
                p.isFilterBitmap = true
                p.color = -0x78000000
                val m = Matrix()
                val scaleX = (mIconSize - mShadowOffset * 2).toFloat() / bmp.width
                val scaleY = (mIconSize - mShadowOffset * 2).toFloat() / bmp.height
                val scale = max(scaleX.toDouble(), scaleY.toDouble()).toFloat()
                m.preScale(scale, scale)
                m.postTranslate((mShadowOffset * 2).toFloat(), (mShadowOffset * 2).toFloat())
                c.drawBitmap(alpha, m, p)
                m.postTranslate(-mShadowOffset.toFloat(), -mShadowOffset.toFloat())
                c.drawBitmap(bmp, m, null)
            } finally {
                alpha.recycle()
            }

            return newBmp
        }
    }

    class WidgetTheme(context: Context?) {
        //			Resources res = context.getResources();
        val timeColor: Int = -0x2f2f30
        val tempColor: Int = -0x53a7
        val rainColor: Int = -0x713801
        val humidColor: Int = -0x710072
        val textColor: Int = -0x1f1f20
    }

    companion object {
        val TAG: String = WidgetUpdateService::class.java.simpleName
        const val ACTION_MANUAL_UPDATE: String = "manualUpdate"

        private val bmpNames = arrayOf(
            "psun", "psnow", "psleet",
            "prain_light", "prain_gusty",
            "prain", "pmoon", "pclouds"
        )

        private val bmpIds = intArrayOf(
            R.drawable.psun, R.drawable.psnow, R.drawable.psleet,
            R.drawable.prain, R.drawable.prain_gusty,
            R.drawable.prain, R.drawable.pmoon, R.drawable.pclouds
        )

        fun getBitmapIndexFromUrl(c: Context?, url: String?): Int {
            return getBitmapIndexFromUrl_old(c, url)
        }

        fun getBitmapIndexFromUrl_old(c: Context?, url: String?): Int {
            if (url != null) {
                for (j in bmpNames.indices) {
                    if (url.contains(bmpNames[j])) {
                        return bmpIds[j]
                    }
                }
            }
            return -1
        }
    }
}
