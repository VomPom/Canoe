package com.voxeldev.canoe.compose.ui.leaderboards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.voxeldev.canoe.compose.ui.components.Error
import com.voxeldev.canoe.compose.ui.components.Loader
import com.voxeldev.canoe.leaderboards.Leaderboards
import com.voxeldev.canoe.leaderboards.api.GeneralizedUser
import com.voxeldev.canoe.leaderboards.api.LeaderboardEntry
import com.voxeldev.canoe.leaderboards.integration.LeaderboardsComponent
import com.voxeldev.canoe.utils.providers.string.StringResourceProvider
import org.koin.compose.koinInject

/**
 * @author nvoxel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardsContent(component: LeaderboardsComponent) {
    val model by component.model.subscribeAsState()
    val stringResourceProvider = koinInject<StringResourceProvider>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Leaderboards") },
                actions = {
                    IconButton(onClick = { component.onToggleFilterBottomSheet() }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filters")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
            ) {
                if (model.isLoading) Loader()

                model.errorText?.let {
                    Error(
                        message = it,
                        shouldShowRetry = true,
                        retryCallback = component::onReloadClicked
                    )
                }

                model.leaderboardsModel?.let {
                    if (it.currentUser.rank != null) {
                        OutlinedCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            UserCard(generalizedUser = it.currentUser, stringResourceProvider = stringResourceProvider)
                        }
                    }

                    LeaderboardsList(
                        leaderboards = it.data,
                        onItemClicked = component::onItemClicked,
                        stringResourceProvider = stringResourceProvider,
                    )

                    FilterBottomSheet(
                        isVisible = model.filterBottomSheetModel.active,
                        onDismissRequest = component::onToggleFilterBottomSheet,
                        selectedLanguage = model.filterBottomSheetModel.selectedLanguage,
                        languages = model.filterBottomSheetModel.languages,
                        onSelectLanguage = component::onSelectLanguage,
                        isHireable = model.filterBottomSheetModel.hireable,
                        onSelectHireable = component::onSelectHireable,
                        selectedCountry = model.filterBottomSheetModel.selectedCountryCode,
                        countries = model.filterBottomSheetModel.countryCodes,
                        onSelectCountry = component::onSelectCountryCode,
                        onResetFilters = component::onResetFilters,
                    )
                }
            }
        }
    )
}

@Composable
private fun LeaderboardsList(
    leaderboards: List<LeaderboardEntry>,
    onItemClicked: (profileUrl: String) -> Unit,
    stringResourceProvider: StringResourceProvider,
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(leaderboards) { entry ->
            LeaderboardsListItem(
                entry = entry,
                onItemClicked = onItemClicked,
                stringResourceProvider = stringResourceProvider
            )
        }
    }
}

@Composable
private fun LeaderboardsListItem(
    entry: LeaderboardEntry,
    onItemClicked: (profileUrl: String) -> Unit,
    stringResourceProvider: StringResourceProvider,
) {
    ElevatedCard(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .clickable { onItemClicked(stringResourceProvider.getWakaTimeProfileBaseUrl() + entry.user.id) },
    ) {
        UserCard(generalizedUser = entry, stringResourceProvider = stringResourceProvider)
    }
}

@Composable
private fun UserCard(
    generalizedUser: GeneralizedUser,
    stringResourceProvider: StringResourceProvider,
) {
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .padding(end = 32.dp)
                .size(size = 64.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(data = "${stringResourceProvider.getWakaTimePhotoBaseUrl()}${generalizedUser.user.id}")
                .transformations(CircleCropTransformation())
                .build(),
            contentDescription = "Avatar",
            loading = {
                CircularProgressIndicator()
            }
        )

        Column {
            EntryProperty(
                text = "#${generalizedUser.rank}",
                style = MaterialTheme.typography.headlineLarge
            )
            EntryProperty(
                text = generalizedUser.user.displayName ?: generalizedUser.user.username ?: generalizedUser.user.id,
                style = MaterialTheme.typography.titleLarge
            )
            generalizedUser.user.city?.title?.let { EntryProperty(text = it) }
            generalizedUser.runningTotal?.languages?.let { languages ->
                EntryProperty(
                    text = languages.take(LANGUAGE_LIMIT).joinToString(separator = ", ") { it.name }
                )
            }
            generalizedUser.runningTotal?.humanReadableDailyAverage?.let { EntryProperty(text = "Daily average: $it") }
            generalizedUser.runningTotal?.humanReadableTotal?.let { EntryProperty(text = "Total: $it") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    selectedLanguage: String?,
    languages: List<Leaderboards.PredefinedLanguageModel>,
    onSelectLanguage: (String) -> Unit,
    isHireable: Boolean?,
    onSelectHireable: (Boolean) -> Unit,
    selectedCountry: String?,
    countries: List<Leaderboards.PredefinedCountryModel>,
    onSelectCountry: (String) -> Unit,
    onResetFilters: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(all = 16.dp)
            ) {
                Text(text = "Filters", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.size(size = 16.dp))

                Text(text = "Programming Language")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(languages) { language ->
                        Chip(
                            value = language.name,
                            isSelected = selectedLanguage == language.value,
                            onClick = { onSelectLanguage(language.value) }
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = selectedLanguage ?: "",
                    onValueChange = onSelectLanguage,
                    label = { Text(text = "Programming Language") },
                )

                Spacer(modifier = Modifier.size(size = 16.dp))

                Text(text = "Country")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(countries) { country ->
                        Chip(
                            value = country.name,
                            isSelected = selectedCountry == country.value,
                            onClick = { onSelectCountry(country.value) }
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = selectedCountry ?: "",
                    onValueChange = onSelectCountry,
                    label = { Text(text = "2-letter country code") },
                )

                Spacer(modifier = Modifier.size(size = 16.dp))

                Text(text = "Is hireable?")
                Switch(checked = isHireable ?: false, onCheckedChange = { onSelectHireable(it) })

                Spacer(modifier = Modifier.size(size = 8.dp))

                Button(onClick = onResetFilters) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset filters")
                    Text(text = "Reset filters")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        modifier = Modifier.padding(horizontal = 4.dp),
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = value) }
    )
}

@Composable
private fun EntryProperty(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Text(
        modifier = modifier
            .padding(vertical = 4.dp),
        text = text,
        style = style
    )
}

private const val LANGUAGE_LIMIT = 5