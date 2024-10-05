package nikeno.Tenki.appwidget.weatherwidget

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
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import nikeno.Tenki.MainActivity
import nikeno.Tenki.R
import nikeno.Tenki.TenkiWidgetProvider
import nikeno.Tenki.feature.fetcher.WeatherFetcher
import nikeno.Tenki.feature.weather.YahooWeather
import nikeno.Tenki.feature.weather.YahooWeather.Hour
import nikeno.Tenki.util.PendingIntentCompat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes

private const val TAG = "WeatherWidgetWorker"

private const val WORK_UPDATE_NAME = "repeat"
fun setupWork(context: Context) {
    val rr = PeriodicWorkRequestBuilder<WeatherWidgetWorker>(
        2, TimeUnit.HOURS
    )
        .addTag("repeat")
        .build()

    val wm = WorkManager.getInstance(context)
    wm.enqueueUniquePeriodicWork(
        WORK_UPDATE_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        rr
    )
}

fun clearWork(context: Context) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork(WORK_UPDATE_NAME)
}

class WeatherWidgetWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private val tags = workerParameters.tags

    override fun doWork(): Result {

        Log.d(TAG, "doWork tags=$tags")

        return runBlocking {
            updateWidgets(applicationContext, false)
        }
    }

    private val fetcher = WeatherWidgetComponent().fetcher

    private suspend fun updateWidgets(context: Context, isManualUpdate: Boolean): Result {
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
        var failed = 0
        for (id in ids) {
            try {
                updateWidget(context, manager, id, theme, false)
            } catch (e: Exception) {
                e.printStackTrace()
                failed++
            }
        }
        if (failed == 0) {
            return Result.success()
        }
        return Result.failure()
    }

    @Throws(Exception::class)
    private suspend fun updateWidget(
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

    private fun getManualReloadPendingIntent(context: Context?): PendingIntent {
        val i = Intent(context, TenkiWidgetProvider::class.java)
        i.setAction(TenkiWidgetProvider.ACTION_UPDATE)
        return PendingIntent.getBroadcast(
            context, 0, i,
            PendingIntentCompat.FLAG_MUTABLE
        )
    }

    private fun createErrorView(context: Context, e: Throwable): RemoteViews {
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

    private fun createProgressView(context: Context): RemoteViews {
        val views = RemoteViews(
            context.packageName,
            R.layout.widget_progress
        )
        views.setOnClickPendingIntent(R.id.progress, getManualReloadPendingIntent(context))
        return views
    }

    private fun buildUpdate(
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
                            val bmp = mIconManager.getIcon(imageUrl)
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

    private val mIconManager: WeatherIconManager = WeatherIconManager(context)

    internal class WeatherIconManager(private val mContext: Context) {
        val fetcher = WeatherWidgetComponent().fetcher
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