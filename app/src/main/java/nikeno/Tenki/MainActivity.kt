package nikeno.Tenki

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.Toast
import nikeno.Tenki.YahooWeather.Day
import nikeno.Tenki.YahooWeather.WeeklyDay
import nikeno.Tenki.activity.AreaSelectActivity
import nikeno.Tenki.activity.HelpActivity
import nikeno.Tenki.dialog.DisplaySettingsDialog
import nikeno.Tenki.task.Callback
import nikeno.Tenki.task.GetYahooWeatherTask
import nikeno.Tenki.view.TextTableView
import nikeno.Tenki.view.TextTableView.CellBitmapHandler
import nikeno.Tenki.viewbinding.ActivityMainBinding
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : Activity(), DisplaySettingsDialog.Listener {
    private var mData: YahooWeather? = null
    private var mDataTime: Long = 0
    private var mColorTempText: Int = 0
    private var mColorHumidityText: Int = 0
    private var mColorDateBg: Int = 0
    private var mColorDateBgDisabled: Int = 0
    private var mColorTextDisabled: Int = 0
    private var mColorMaxTempText: Int = 0
    private var mColorMinTempText: Int = 0

    private var mDownloadTask: GetYahooWeatherTask? = null
    private lateinit var mPrefs: Prefs
    private var mPrefUrl: String? = null
    private lateinit var mBinding: ActivityMainBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        TenkiApp.applyActivityTheme(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mBinding = ActivityMainBinding(this)
        mPrefs = (application as TenkiApp).prefs

        mPrefUrl = if (intent.dataString != null) {
            Utils.httpsUrl(intent.dataString)
        } else {
            Utils.httpsUrl(mPrefs.currentAreaUrl)
        }

        val ta = theme.obtainStyledAttributes(R.styleable.WeatherTable)
        mColorTempText = ta.getColor(R.styleable.WeatherTable_colorTempText, 0)
        mColorHumidityText = ta.getColor(R.styleable.WeatherTable_colorHumidityText, 0)
        mColorDateBg = ta.getColor(R.styleable.WeatherTable_colorTableDateBackground, 0)
        mColorDateBgDisabled =
            ta.getColor(R.styleable.WeatherTable_colorTableDateBackgroundDisabled, 0)
        mColorTextDisabled = ta.getColor(R.styleable.WeatherTable_colorTableTextDisabled, 0)
        mColorMaxTempText = ta.getColor(R.styleable.WeatherTable_colorMaxTempText, 0)
        mColorMinTempText = ta.getColor(R.styleable.WeatherTable_colorMinTempText, 0)
        ta.recycle()

        loadCache(mPrefUrl)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mDownloadTask != null) {
            mDownloadTask!!.cancel(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val mi = menu.findItem(R.id.darkTheme)
        mi?.setChecked(mPrefs.theme == Prefs.ThemeNames.DARK)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_reload) {    // リロード
            reload()
        } else if (itemId == R.id.menu_pref) {    // 地域変更
            onClickChangeArea(null)
        } else if (itemId == R.id.menu_help) {    // ヘルプ
            val i = Intent(applicationContext, HelpActivity::class.java)
            startActivity(i)
        } else if (itemId == R.id.display_settings) { //                showDisplaySettings();
            DisplaySettingsDialog(this).show()
        } else if (itemId == R.id.darkTheme) {
            mPrefs.theme = if (item.isChecked) Prefs.ThemeNames.DEFAULT else Prefs.ThemeNames.DARK
            recreate()
        }
        return true
    }

    private fun showDisplaySettings() {
        val dlg = AlertDialog.Builder(this)
            .setPositiveButton(R.string.close, null)
            .create()
        dlg.setOwnerActivity(this)
        dlg.setTitle(R.string.display_settings)
        dlg.setView(dlg.layoutInflater.inflate(R.layout.display_settings, null))
        val showWeatherIcon = dlg.findViewById<CheckBox>(R.id.show_weather_icon)
        showWeatherIcon.setOnCheckedChangeListener { compoundButton, b -> parent }
        dlg.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult")
        when (requestCode) {
            REQUEST_AREA -> if (resultCode == RESULT_OK) {
                mPrefUrl = Utils.httpsUrl(data.getStringExtra("url"))
                mPrefs.currentAreaUrl = mPrefUrl
                reload()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (mData != null) {
            setData(mData, mDataTime)
            val elapsed = System.currentTimeMillis() - mDataTime
            if (elapsed < 0 || elapsed > 5 * DateUtils.MINUTE_IN_MILLIS) {
                reload()
            }
        } else {
            reload()
        }
    }

    override fun onSearchRequested(): Boolean {
        onClickChangeArea(null)
        return true
    }

    fun onClickChangeArea(v: View?) {
        val i = Intent(applicationContext, AreaSelectActivity::class.java)
        startActivityForResult(i, REQUEST_AREA)
    }

    fun onClickOpenBrowser(v: View?) {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(mPrefUrl))
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadCache(url: String?) {
        val entry = TenkiApp.from(this).downloader.getCache(
            url,
            System.currentTimeMillis() - 24 * DateUtils.HOUR_IN_MILLIS
        )
        if (entry != null) {
            try {
                mData = YahooWeather.parse(entry.data)
                mDataTime = entry.time
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, e.message.toString())
            }
        }
    }

    // リロードする
    private fun reload() {
        if (mDownloadTask != null) {
            return
        }

        mBinding.errorGroup.visibility = View.GONE
        mBinding.progress.visibility = View.VISIBLE
        mDownloadTask = GetYahooWeatherTask(
            TenkiApp.from(this).downloader,
            mPrefUrl!!, object : Callback<YahooWeather?>() {
                override fun onSuccess(result: YahooWeather?) {
                    mData = result
                    mDataTime = System.currentTimeMillis()
                    setData(mData, mDataTime)
                }

                override fun onError(error: Throwable) {
                    mBinding.errorGroup.visibility = View.VISIBLE
                    mBinding.errorGroup.startAnimation(
                        AnimationUtils.loadAnimation(
                            this@MainActivity,
                            android.R.anim.fade_in
                        )
                    )
                }

                override fun onFinish() {
                    super.onFinish()
                    mBinding.progress.visibility = View.GONE
                    mDownloadTask = null
                }
            })
        mDownloadTask!!.execute()
    }

    private fun setDayData(v: TextTableView, data: Day, textSize: Float) {
        val nowJapan = Calendar.getInstance(Locale.JAPAN)
        val now = nowJapan.time.time - 3 * DateUtils.HOUR_IN_MILLIS
        val baseTime = data.date.time

        val showWeatherIcon = mPrefs[Prefs.SHOW_WEATHER_ICON]
        val showWeatherIconLabel = mPrefs[Prefs.SHOW_WEATHER_ICON_LABEL]
        val showTemperature = mPrefs[Prefs.SHOW_TEMPERATURE]
        val showHumidity = mPrefs[Prefs.SHOW_HUMIDITY]
        val showPrecipitation = mPrefs[Prefs.SHOW_PRECIPITATION]
        val showWind = mPrefs[Prefs.SHOW_WIND]

        val columns = data.hours.size

        var r = 1
        val timeRow = 0
        val weatherRow = (if ((showWeatherIcon || showWeatherIconLabel)) r++ else -1)
        val tempRow = (if (showTemperature) r++ else -1)
        val humidityRow = (if (showHumidity) r++ else -1)
        val precipitationRow = (if (showPrecipitation) r++ else -1)
        val windRow = (if (showWind) r++ else -1)
        v.setSize(columns, r)

        v.getRow(timeRow).setTextSize(textSize) // 時間
        if (weatherRow != -1) {
            v.getRow(weatherRow).setTextSize(textSize * 0.75f) // 天気
        }
        if (tempRow != -1) {
            v.getRow(tempRow).setTextSize(textSize) // 温度
            v.getRow(tempRow).setTextColor(mColorTempText)
        }
        if (humidityRow != -1) {
            v.getRow(humidityRow).setTextSize(textSize) // 湿度
            v.getRow(humidityRow).setTextColor(mColorHumidityText)
        }
        if (precipitationRow != -1) {
            v.getRow(precipitationRow).setTextSize(textSize) // 雨量
        }
        if (windRow != -1) {
            v.getRow(windRow).setTextSize(textSize * 0.75f) // 風
        }

        v.getRow(1).setIconWidth((textSize * 2f).toInt())

        val downloader = ImageDownloader.getInstance(this)

        for (x in 0 until columns) {
            val h = data.hours[x]

            val enabled = (baseTime + h.hour * DateUtils.HOUR_IN_MILLIS) > now
            val imageUrl = h.getImageUrl(enabled)

            v.getCell(x, timeRow).setText(h.hour.toString())

            if (weatherRow != -1 && showWeatherIcon) {
                downloader.setImage(
                    imageUrl,
                    CellBitmapHandler(v, v.getCell(x, weatherRow))
                )
            }
            if (weatherRow != -1 && showWeatherIconLabel) v.getCell(x, weatherRow).setText(h.text)
            if (tempRow != -1) v.getCell(x, tempRow).setText(h.temp)
            if (humidityRow != -1) v.getCell(x, humidityRow).setText(h.humidity)
            if (precipitationRow != -1) v.getCell(x, precipitationRow).setText(h.rain)
            if (windRow != -1) {
                v.getCell(x, windRow).setText(
                    if (h.wind.length < 3) h.wind else """
     ${h.wind.substring(0, 2)}
     ${h.wind.substring(2)}
     """.trimIndent()
                )
            }

            v.getCell(x, timeRow)
                .setBackgroundColor(if (enabled) mColorDateBg else mColorDateBgDisabled)
            if (!enabled) {
                v.getColumn(x).setTextColor(mColorTextDisabled)
            }
        }
        v.requestLayout()
        v.invalidate()
    }


    private fun setWeekData(v: TextTableView, data: Array<WeeklyDay>, textSize: Float) {
        val res = resources
        val isLandscape = res.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val downloader = ImageDownloader.getInstance(this)
        v.setSize(data.size, 4)
        v.getRow(0).setBackgroundColor(mColorDateBg)
        //        v.getRow(1).setIconWidth(res.getDimensionPixelSize(R.dimen.weekWeatherIconWidth));
        v.all.setTextSize(textSize)
        v.getRow(1).setIconWidth((textSize * 2.5f).toInt())
        if (!isLandscape) {
            v.getRow(1).setTextSize(textSize * 0.75f)
        }

        for (x in data.indices) {
            val i = data[x]

            v.getCell(x, 0).setText(if (isLandscape) i.date.replace("\n", "") else i.date)

            downloader.setImage(
                i.imageUrl,
                CellBitmapHandler(v, v.getCell(x, 1))
            )

            v.getCell(x, 1).setText(i.text)

            val ss = SpannableString(i.tempMax + "/" + i.tempMin)
            ss.setSpan(
                ForegroundColorSpan(mColorMaxTempText),
                0, i.tempMax.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ss.setSpan(
                ForegroundColorSpan(mColorMinTempText),
                ss.length - i.tempMin.length, ss.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            v.getCell(x, 2).setText(ss)
            v.getCell(x, 3).setText(i.rain)
        }
    }

    private var mLastData: YahooWeather? = null
    private var mLastDataTime: Long = 0

    private fun setData(data: YahooWeather?, time: Long) {
        title = getString(R.string.mainTitleFormat, data!!.areaName)

        mLastData = data
        mLastDataTime = time

        val dm = resources.displayMetrics
        var textSize = (dm.widthPixels / 8f / 3f)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textSize /= 2f
        }
        mBinding.todayHeader.text = getDateText(data.today.date)
        mBinding.tomorrowHeader.text = getDateText(data.tomorrow.date)

        mBinding.todayHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        mBinding.tomorrowHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        if (mBinding.weekHeader != null) {
            mBinding.weekHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }

        setDayData(mBinding.todayTable2, data.today, textSize)
        setDayData(mBinding.tomorrowTable2, data.tomorrow, textSize)
        setWeekData(mBinding.weekTable2, data.days, textSize)
        mBinding.time.text = DateUtils.formatDateTime(
            this, time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
    }

    private fun getDateText(d: Date): String {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
        c.time = d
        val day = c[Calendar.DAY_OF_WEEK] - 1
        return String.format(
            Locale.ENGLISH, "%d月%d日(%s)",
            c[Calendar.MONTH] + 1,
            c[Calendar.DATE],
            WeekStr.substring(day, day + 1)
        )
    }

    fun onClickErrorMessage(v: View?) {
        reload()
    }

    override fun onDisplaySettingChanged() {
        if (mLastData != null) {
            setData(mLastData, mLastDataTime)
        }
    }

    companion object {
        const val REQUEST_AREA: Int = 1
        private const val TAG = "Tenki_MainActivity"
        private const val WeekStr = "日月火水木金土"
    }
}