package com.mobileorienteering.ui.screens.runs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.screens.runs.components.ActivityCard
import com.mobileorienteering.ui.core.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunsScreen(
    navController: NavController,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val isLoading by remember { viewModel.isLoading }
    val error by remember { viewModel.error }
    val searchQuery by remember { viewModel.searchQuery }
    val sortOrder by remember { viewModel.sortOrder }

    val activities by viewModel.filteredActivities.collectAsState()
    val maps by viewModel.getAllMaps().collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchExpanded,
                        label = "search_animation"
                    ) { expanded ->
                        if (expanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(Strings.Runs.searchHint) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.background,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.background
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                )
                            )
                        } else {
                            Text(Strings.Nav.runs)
                        }
                    }
                },
                navigationIcon = {
                    if (!isSearchExpanded) {
                        IconButton(
                            onClick = { isSearchExpanded = true }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = Strings.Accessibility.search
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchExpanded) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = false
                                viewModel.updateSearchQuery("")
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = Strings.Accessibility.closeSearch
                            )
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sort),
                                    contentDescription = Strings.Accessibility.sort
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Strings.Runs.newestFirst) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.DATE_DESC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == SortOrder.DATE_DESC) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(Strings.Runs.oldestFirst) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.DATE_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == SortOrder.DATE_ASC) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(Strings.Runs.longestDistance) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.DISTANCE_DESC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == SortOrder.DISTANCE_DESC) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(Strings.Runs.shortestDistance) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.DISTANCE_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == SortOrder.DISTANCE_ASC) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(Strings.Runs.sortTitleAz) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.TITLE_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == SortOrder.TITLE_ASC) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }.apply {
                    LaunchedEffect(error) {
                        error?.let {
                            showSnackbar(it)
                            viewModel.clearError()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (maps.isEmpty()) {
                        Text(
                            Strings.Runs.emptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            Strings.Library.emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { navController.navigate(AppScreen.Map.route) },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(Strings.Library.createFirstMap)
                        }
                    } else {
                        Text(
                            Strings.Runs.emptyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            Strings.Runs.emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { navController.navigate(AppScreen.Library.route) },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(Strings.Runs.goToMaps)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities, key = { it.id }) { activity ->
                    val activityMap = maps.find { it.id == activity.mapId }
                    ActivityCard(
                        activity = activity,
                        mapName = activityMap?.name,
                        controlPointCount = activityMap?.controlPoints?.size,
                        onDelete = {
                            viewModel.deleteActivity(activity.id)
                        },
                        onClick = {
                            navController.navigate(AppScreen.RunDetails.createRoute(activity.id))
                        }
                    )
                }
            }
        }

    }
}
