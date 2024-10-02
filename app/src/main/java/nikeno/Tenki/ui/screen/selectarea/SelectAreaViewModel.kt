package nikeno.Tenki.ui.screen.selectarea

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nikeno.Tenki.Area
import nikeno.Tenki.Prefs
import nikeno.Tenki.feature.weather.YahooWeatherClient

data class SelectAreaState(
    val keyword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val savedAreaList: List<Area> = emptyList(),
    val foundAreaList: List<Area>? = null,
)

class SelectAreaViewModel(private val prefs: Prefs, private val client: YahooWeatherClient) :
    ViewModel() {

    private val mState = MutableStateFlow(SelectAreaState())
    val state = mState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.recentAreaList.collect {
                mState.value = mState.value.copy(savedAreaList = it)
            }
        }
    }

    fun setKeyword(value: String) {
        mState.value = mState.value.copy(keyword = value)
    }

    fun search() {
        if (mState.value.loading) return

        val keyword = mState.value.keyword
        mState.value = mState.value.copy(
            loading = true,
            error = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = client.searchAddress(keyword)
                Log.d(TAG, "検索結果: $result")
                mState.value = mState.value.copy(
                    foundAreaList = result,
                    loading = false
                )
            } catch (e: Exception) {
                mState.value = mState.value.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }

    companion object {
        const val TAG = "SelectAreaViewModel"
    }
}