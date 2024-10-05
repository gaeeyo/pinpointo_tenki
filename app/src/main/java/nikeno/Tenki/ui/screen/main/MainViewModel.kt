package nikeno.Tenki.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import nikeno.Tenki.feature.fetcher.WeatherFetcher
import nikeno.Tenki.feature.weather.YahooWeather
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MainViewModel(private val fetcher: WeatherFetcher) : ViewModel() {

    data class MainViewState(
        val now: Long = Clock.System.now().toEpochMilliseconds(),
        val url: String? = null,
        val data: YahooWeather? = null,
        val dataTime: Long = 0,
        val isCache: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val mState = MutableStateFlow(MainViewState())
    val state = mState.asStateFlow()


    private val isLoading
        get() = mState.value.isLoading

    // compose側で collectAsStateWithLifecycle() する
    // アプリが Foreground になったとき、データが古ければ再読み込みする
    val active = flow<Unit> {
        if (isDataOutdated && !isLoading) {
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
                error = null,
                isCache = false,
            )
            requestData()
        }
    }

    fun requestData() {
        val s = state.value
        if (s.isLoading) {
            Log.d(TAG, "すでに読み込み中")
            return
        }

        if (s.data == null && s.error == null) {
            Log.d(TAG, "データがないので先にキャッシュ読み込み")
            viewModelScope.launch {
                fetchData(Clock.System.now().minus(12.hours).toEpochMilliseconds())
            }
        } else {
            Log.d(TAG, "キャッシュを使わず読み込み")
            viewModelScope.launch {
                fetchData(null)
            }
        }
    }

    suspend private fun fetchData(since: Long?) {
        val url = mState.value.url ?: return

        withContext(Dispatchers.Main) {
            mState.value = mState.value.copy(
                now = Clock.System.now().toEpochMilliseconds(),
                isLoading = true, error = null
            )
        }
        val data = fetcher.getWeather(url, since)
        withContext(Dispatchers.Main) {
            when (data) {
                is WeatherFetcher.WeatherResult.Error -> {
                    mState.value = mState.value.copy(
                        isCache = false,
                        isLoading = false,
                        error = data.error.message
                    )
                }

                is WeatherFetcher.WeatherResult.Success -> {
                    mState.value = mState.value.copy(
                        data = data.data,
                        dataTime = data.time,
                        isCache = data.isCache,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private val isDataOutdated: Boolean
        get() = mState.value.dataTime < Clock.System.now().minus(15.minutes).toEpochMilliseconds()

    companion object {
        const val TAG = "MainViewModel"
    }
}