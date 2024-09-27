package nikeno.Tenki.ui.main

import android.app.Application
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nikeno.Tenki.TenkiApp
import nikeno.Tenki.YahooWeather
import nikeno.Tenki.feature.weather.getYahooWeather

class MainViewModel(application: Application) : AndroidViewModel(application) {
    data class MainViewState(
        val url: String? = null,
        val data: YahooWeather? = null,
        val dataTime: Long = 0,
        val isCache: Boolean = false,
        val preferCache: Boolean = true,
        val loading: Boolean = false,
        val error: String? = null
    )

    private val mState = MutableStateFlow(MainViewState())
    val state = mState.asStateFlow()

    fun setUrl(url: String) {
        if (mState.value.url != url) {
            mState.value = mState.value.copy(
                url = url,
                data = null,
                dataTime = 0,
                preferCache = true,
                isCache = false,
            )
            requestData()
        }
    }

    fun requestData() {
        val s = state.value
        if (s.loading) return

        if (s.data == null) {
            if (s.preferCache) {
                mState.value = mState.value.copy(preferCache = false)
                loadCache()
                reload()
            } else {
                reload()
            }
        } else {
            val elapsed = System.currentTimeMillis() - s.dataTime
            if (elapsed < 0 || elapsed > 5 * DateUtils.MINUTE_IN_MILLIS) {
                reload()
            }
        }
    }

    fun reload() {
        val url = mState.value.url ?: return

        mState.value = mState.value.copy(
            loading = true,
            error = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = getYahooWeather(
                    TenkiApp.from(getApplication()).downloader,
                    url
                )
                mState.value = mState.value.copy(
                    data = data,
                    dataTime = System.currentTimeMillis(),
                    isCache = false,
                    loading = false,
                    error = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                mState.value = mState.value.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadCache() {
        val entry = TenkiApp.from(getApplication()).downloader.getCache(
            mState.value.url,
            System.currentTimeMillis() - 24 * DateUtils.HOUR_IN_MILLIS
        )
        if (entry != null) {
            try {
                mState.value = mState.value.copy(
                    data = YahooWeather.parse(entry.data),
                    dataTime = entry.time,
                    isCache = true,
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(javaClass.simpleName, e.message.toString())
            }
        }
    }
}