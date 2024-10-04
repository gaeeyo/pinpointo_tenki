package nikeno.Tenki.ui.screen.main

import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import nikeno.Tenki.feature.weather.YahooWeather
import nikeno.Tenki.feature.weather.YahooWeatherClient
import kotlin.time.Duration.Companion.minutes

class MainViewModel(private val client: YahooWeatherClient) : ViewModel() {
    data class MainViewState(
        val now: Long = Clock.System.now().toEpochMilliseconds(),
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

    // compose側で collectAsStateWithLifecycle() する
    // アプリが Foreground になったとき、データが古ければ再読み込みする
    val active = flow<Unit> {
        if (isDataOutdated) {
            Log.d(TAG, "データが無効なので再読み込み")
            requestData()
        }
    }

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
                if (isDataOutdated) {
                    reload()
                }
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

    private fun reload() {
        val url = mState.value.url ?: return

        mState.value = mState.value.copy(
            now = Clock.System.now().toEpochMilliseconds(), loading = true, error = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = client.getYahooWeather(url)
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
                    loading = false, error = e.message
                )
            }
        }
    }

    private fun loadCache() {
        val url = mState.value.url ?: return

        val weather = client.getCachedWeather(
            url, System.currentTimeMillis() - 12 * DateUtils.HOUR_IN_MILLIS
        )
        if (weather != null) {
            try {
                mState.value = mState.value.copy(
                    data = weather.data,
                    dataTime = weather.time,
                    isCache = true,
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, e.message.toString())
            }
        }
    }

    private val isDataOutdated: Boolean
        get() = mState.value.dataTime < Clock.System.now().minus(15.minutes).toEpochMilliseconds()

    companion object {
        const val TAG = "MainViewModel"
    }
}