package nikeno.Tenki.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nikeno.Tenki.ImageDownloader
import nikeno.Tenki.R
import nikeno.Tenki.YahooWeather
import nikeno.Tenki.ui.theme.LocalWeatherTheme
import kotlin.math.max
import kotlin.math.roundToInt


private val TAG = "MainScreen"

enum class ClickEvent {
    OPEN_BROWSER, CHANGE_AREA, HELP, RELOAD, CHANGE_THEME
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, onClick: (ClickEvent) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }

    val state = vm.state.collectAsState().value
    val context = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainScreenAppBar(state, onClick)
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MainScreenContent(state, onClick)
            if (state.loading) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            LaunchedEffect(state.error) {
                Log.d(TAG, "エラーの有無: ${!state.error.isNullOrEmpty()}")

                if (!state.error.isNullOrEmpty()) {
                    if (snackbarHostState.showSnackbar(
                            state.error,
                            actionLabel = context.getString(R.string.retry),
                        ) == SnackbarResult.ActionPerformed
                    ) {
                        vm.setError(null)
                        onClick(ClickEvent.RELOAD)
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenAppBar(
    state: MainViewModel.MainViewState,
    onClick: (ClickEvent) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        modifier = Modifier.shadow(8.dp),
        title = {
            Text(state.data?.areaName?.let { "${it}の天気" } ?: "")
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        actions = {
            IconButton(onClick = { onClick(ClickEvent.RELOAD) }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = ""
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Localized description"
                )
            }
            fun onMenuClick(event: ClickEvent) {
                onClick(event)
                showMenu = false
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_pref)) },
                    onClick = {
                        onMenuClick(ClickEvent.CHANGE_AREA)
                    })
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.dark_theme)) },
                    onClick = {
                        onMenuClick(ClickEvent.CHANGE_THEME)
                    })
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_help)) },
                    onClick = {
                        onMenuClick(ClickEvent.HELP)
                    })

            }
        },
    )

}

@Composable
fun MainScreenContent(
    state: MainViewModel.MainViewState, onClick: (ClickEvent) -> Unit,
) {
    val tableMargin = 8.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(tableMargin)
    ) {

        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current

            Text(
                if (state.dataTime != 0L) formatUpdateTime(context, state.dataTime) else "",
                modifier = Modifier.padding(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (state.data != null) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 600.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(tableMargin)
                    ) {
                        WeatherDay(state.data.today, state.now)
                        WeatherDay(state.data.tomorrow, state.now)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tableMargin)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            WeatherDay(state.data.today, state.now)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            WeatherDay(state.data.tomorrow, state.now)
                        }
                    }
                }
            }
            WeatherWeek(state.data.days)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()

        ) {
            ElevatedButton(onClick = { onClick(ClickEvent.OPEN_BROWSER) }) {
                Text(stringResource(R.string.main_open_browser))
            }
            ElevatedButton(onClick = { onClick(ClickEvent.CHANGE_AREA) }) {
                Text(stringResource(R.string.main_pref))
            }
        }
    }
}

fun formatUpdateTime(context: Context, time: Long): String {
    return "更新 " + DateUtils.formatDateTime(
        context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )

}


fun Modifier.drawBottomLine(color: Color): Modifier {
    return drawWithContent {
        drawContent()
        drawLine(color, Offset(0f, size.height), Offset(size.width, size.height))
    }
}

fun Modifier.drawRightLine(color: Color): Modifier {
    return drawWithContent {
        drawContent()
        drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height))
    }
}

@Composable
fun WeatherDay(data: YahooWeather.Day, now: Long) {
    val context = LocalContext.current
    val dateText = DateUtils.formatDateTime(
        context,
        data.date.time,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
    )
    // 表の先頭の時間と現在時刻をhour単位にする
    val startH = data.date.time / (60 * 60 * 1000)
    val nowH = (now / (60 * 60 * 1000) / 3) * 3
    val wt = LocalWeatherTheme.current

    fun isPast(h: Int) = startH + h < nowH

    val dateRowModifier = Modifier
        .fillMaxWidth()
        .drawWithContent {
            // 日付の背景を塗る
            // 過去の色
            val pastRightH = nowH - startH
            if (pastRightH in 0..24) {
                drawRect(
                    wt.pastBackground, Offset(0f, 0f),
                    Size(pastRightH * size.width / 24, size.height),
                )
            }
            // 未来の色
            val futureLeftH = max(pastRightH, 0)
            if (futureLeftH in 0..24) {
                val newLeft = futureLeftH * size.width / 24
                drawRect(
                    wt.dateBackground, Offset(newLeft, 0f),
                    Size(size.width - newLeft, size.height),
                )
            }
            drawContent()
            // 下線
            drawLine(wt.borderColor, Offset(0f, size.height), Offset(size.width, size.height))
        }
    val rowModifier = Modifier
        .fillMaxWidth()
        .drawWithContent {
            drawContent()
            // 下線
            drawLine(wt.borderColor, Offset(0f, size.height), Offset(size.width, size.height))
        }

    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(dateText, style = MaterialTheme.typography.bodyLarge)
        val colModifier = Modifier.weight(1f)
        Column(modifier = Modifier
            .background(wt.tableBackground)
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val width = size.width
                val cols = data.hours.size
                // 縦線
                for (j in 0..cols) {
                    val x = (width * j / cols)
                        .roundToInt()
                        .toFloat()
                    drawLine(wt.borderColor, Offset(x, 0f), Offset(x, size.height))
                }
                // 上
                drawLine(wt.borderColor, Offset(0f, 0f), Offset(size.width, 0f))
            }) {
            Row(modifier = dateRowModifier) {
                for (hour in data.hours) {
                    Text(
                        "${hour.hour}",
                        modifier = colModifier,
                        color = if (isPast(hour.hour)) wt.pastContent else wt.hour,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(modifier = rowModifier.padding(top = 2.dp)) {
                for (hour in data.hours) {
                    Column(
                        modifier = colModifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeatherIcon(url = hour.getImageUrl(!isPast(hour.hour)))
                        Text(
                            hour.text,

                            color = if (isPast(hour.hour)) wt.pastContent else wt.weather,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(modifier = rowModifier) {
                for (hour in data.hours) {
                    Text(
                        hour.temp,
                        modifier = colModifier,
                        color = if (isPast(hour.hour)) wt.pastContent else wt.temperature,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(modifier = rowModifier) {
                for (hour in data.hours) {
                    Text(
                        hour.humidity,
                        modifier = colModifier,
                        color = if (isPast(hour.hour)) wt.pastContent else wt.humidity,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(modifier = rowModifier) {
                for (hour in data.hours) {
                    Text(
                        hour.rain,
                        modifier = colModifier,
                        color = if (isPast(hour.hour)) wt.pastContent else wt.rain,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val textStyle = MaterialTheme.typography.bodyMedium
                for (hour in data.hours) {
                    Text(
                        hour.wind,
                        style = textStyle,
                        color = if (isPast(hour.hour)) wt.pastContent else wt.wind,
                        modifier = colModifier,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherWeek(data: Array<YahooWeather.WeeklyDay>) {
    val wt = LocalWeatherTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.weekly_text), style = MaterialTheme.typography.bodyLarge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(wt.tableBackground)
                .drawWithContent {
                    drawContent()
                    // 上と縦線
                    drawLine(wt.borderColor, Offset(0f, 0f), Offset(size.width, 0f))
                    for (j in 0..data.size) {
                        val left = j * size.width / data.size
                        drawLine(wt.borderColor, Offset(left, 0f), Offset(left, size.height))
                    }
                }
        ) {

            val rowModifier = Modifier
                .fillMaxWidth()
                .drawBottomLine(wt.borderColor)

            val cellModifier = Modifier
                .weight(1f)
            // 日付
            Row(modifier = rowModifier) {
                for (d in data) {
                    Text(d.date, modifier = cellModifier, textAlign = TextAlign.Center)
                }
            }
            // アイコン、天気
            Row(modifier = rowModifier.padding(top = 4.dp)) {
                val textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                for (d in data) {
                    Column(
                        modifier = cellModifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeatherIcon(url = d.imageUrl)
                        Text(d.text, textAlign = TextAlign.Center, style = textStyle)
                    }
                }
            }
            // 最高気温、最低気温
            Row(modifier = rowModifier) {
                for (d in data) {
                    Text(buildAnnotatedString {
                        pushStyle(SpanStyle(wt.temperatureMax))
                        append(d.tempMax)
                        pop()
                        append("/")
                        pushStyle(SpanStyle(wt.temperatureMin))
                        append(d.tempMin)
                    }, modifier = cellModifier, textAlign = TextAlign.Center)
                }
            }
            // 降水確率
            Row(modifier = rowModifier) {
                for (d in data) {
                    Text(d.rain, modifier = cellModifier, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun WeatherIcon(modifier: Modifier = Modifier, url: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val downloader = ImageDownloader.getInstance(context)
    LaunchedEffect(url) {
        downloader.setImage(url) {
            bitmap = it
        }
    }
    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(), "",
            modifier = modifier,
            contentScale = ContentScale.Inside
        )
    } else {
        Box(modifier = modifier)
    }
}