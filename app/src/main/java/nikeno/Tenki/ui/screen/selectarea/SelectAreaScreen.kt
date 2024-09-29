package nikeno.Tenki.ui.screen.selectarea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import nikeno.Tenki.Area
import nikeno.Tenki.R
import nikeno.Tenki.ui.app.LocalWeatherTheme
import nikeno.Tenki.ui.app.MyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAreaScreen(navController: NavController) {
    val vm: SelectAreaViewModel = viewModel()

    fun onSelectedArea(result: Area) {
        navController.previousBackStackEntry?.savedStateHandle?.set("selectedArea", result)
        navController.popBackStack()
    }

    val state = vm.state.collectAsState().value
    val savedAreaList = state.savedAreaList
    val foundAreaList = state.foundAreaList
    val wt = LocalWeatherTheme.current

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        MyTopBar(title = { Text(stringResource(R.string.area_select_title)) })
    }) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            SearchAreaScreenContent(
                keyword = state.keyword,
                setKeyword = vm::setKeyword,
                savedAreaList = state.savedAreaList,
                foundAreaList = foundAreaList,
                loading = state.loading,
                onSelectArea = ::onSelectedArea,
                onSearch = vm::search
            )
        }
    }
}

@Composable
fun SearchAreaScreenContent(
    keyword: String,
    setKeyword: (String) -> Unit,
    loading: Boolean,
    savedAreaList: List<Area>,
    foundAreaList: List<Area>?,
    onSelectArea: (Area) -> Unit,
    onSearch: () -> Unit,
) {

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = setKeyword,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        stringResource(R.string.area_select_search_empty),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            Button(
                onClick = onSearch, modifier = Modifier.height(48.dp),
                enabled = !loading,
            ) {
                Text(stringResource(R.string.area_select_search))
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                if (foundAreaList != null) {
                    item {
                        Text(
                            stringResource(R.string.searchResult),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    item {
                        if (!loading && foundAreaList.isEmpty()) {
                            Text(
                                stringResource(R.string.area_not_found),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    items(foundAreaList) {
                        ElevatedButton(
                            onClick = { onSelectArea(it) }, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(it.address1)
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (savedAreaList.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.recentlyUsedArea),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(savedAreaList) {
                        ElevatedButton(
                            onClick = { onSelectArea(it) }, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(it.address1)
                            }
                        }
                    }
                }
            }
            if (loading) {
                CircularProgressIndicator()
            }
        }
    }
}