package nikeno.Tenki

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class Prefs(val mPrefs: SharedPreferences) {
    enum class ThemeNames(var value: String) {
        DEFAULT("default"), DARK("dark");

        companion object {
            fun fromValue(value: String?): ThemeNames {
                return when (value) {
                    DARK.value -> ThemeNames.DARK
                    else -> ThemeNames.DEFAULT
                }
            }
        }
    }

    private val mTheme = MutableStateFlow(
        ThemeNames.fromValue(mPrefs.getString(THEME, ThemeNames.DEFAULT.value))
    )

    val theme = mTheme.asStateFlow()

    fun setTheme(theme: ThemeNames) {
        mTheme.value = theme
        mPrefs.edit().putString(THEME, theme.value).apply()
    }

    private val mRecentAreaList = MutableStateFlow<List<Area>>(loadRecentAreaList())

    private fun loadRecentAreaList(): List<Area> {
        val list: MutableList<Area> = ArrayList()

        var data: String?
        for (j in 0 until RECENT_MAX) {
            data = mPrefs.getString(RECENT_PREFIX + j, null)
            if (data != null) {
                val areaData = Area.deserialize(data)
                if (areaData != null) {
                    list.add(areaData)
                }
            }
        }
        return list
    }

    val recentAreaList = mRecentAreaList.asStateFlow()

    fun setRecentAreaList(list: List<Area>) {
        mRecentAreaList.value = list
        val editor = mPrefs.edit()

        for (j in 0 until RECENT_MAX) {
            if (j < list.size) {
                editor.putString(RECENT_PREFIX + j, list[j].serialize())
            } else {
                editor.remove(RECENT_PREFIX + j)
            }
        }
        editor.apply()
    }

    fun addRecentArea(area: Area) {
        val list = mRecentAreaList.value.toMutableList()
        list.remove(area)
        list.add(0, area)
        setRecentAreaList(list)
    }

    fun removeRecentArea(area: Area) {
        val list = mRecentAreaList.value.toMutableList()
        list.remove(area)
        setRecentAreaList(list)
    }

    private val mCurrentAreaUrl = MutableStateFlow(
        mPrefs.getString(
            URL,
            "https://weather.yahoo.co.jp/weather/jp/13/4410/13101.html"
        )
    )

    val currentAreaUrl = mCurrentAreaUrl.asStateFlow()

    private fun setCurrentAreaUrl(value: String) {
        mCurrentAreaUrl.value = value
        mPrefs.edit().putString(URL, value).apply()
    }

    fun setCurrentArea(area: Area) {
        // 設定地域を変更
        setCurrentAreaUrl(area.url)

        // 最近使った地域に登録
        addRecentArea(area)
    }

    fun get(key: BoolValue): Boolean {
        return mPrefs.getBoolean(key.key, key.defaultValue)
    }

    fun set(key: BoolValue, value: Boolean) {
        mPrefs.edit().putBoolean(key.key, value).apply()
    }

    class BoolValue(val key: String, val defaultValue: Boolean)
    companion object {
        @JvmField
        val SHOW_WEATHER_ICON: BoolValue = BoolValue("showWeatherIcon", true)

        @JvmField
        val SHOW_WEATHER_ICON_LABEL: BoolValue = BoolValue("showWeatherIconLabel", true)

        @JvmField
        val SHOW_TEMPERATURE: BoolValue = BoolValue("showTemperature", true)

        @JvmField
        val SHOW_HUMIDITY: BoolValue = BoolValue("showHumidity", true)

        @JvmField
        val SHOW_PRECIPITATION: BoolValue = BoolValue("showPrecipitation", true)

        @JvmField
        val SHOW_WIND: BoolValue = BoolValue("showWind", true)

        const val RECENT_PREFIX: String = "Recent"
        const val URL: String = "url"
        const val THEME: String = "theme"
        private const val RECENT_MAX = 5
    }
}
