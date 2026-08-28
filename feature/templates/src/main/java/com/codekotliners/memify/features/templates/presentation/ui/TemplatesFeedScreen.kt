        package com.codekotliners.memify.features.templates.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekotliners.memify.features.templates.presentation.state.TabState
import com.codekotliners.memify.features.templates.presentation.ui.components.ErrorTab
import com.codekotliners.memify.features.templates.presentation.ui.components.LoadingTab
import com.codekotliners.memify.features.templates.presentation.ui.components.NoContentTab
import com.codekotliners.memify.features.templates.presentation.ui.components.TemplatesGrid
import com.codekotliners.memify.features.templates.presentation.viewmodel.TemplatesFeedViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesFeedScreen(
    authChanged: Boolean,
    onAuthChangedHandled: () -> Unit,
    onLoginClicked: () -> Unit,
    onTemplateSelected: (String) -> Unit,
    viewModel: TemplatesFeedViewModel = hiltViewModel(),
) {
    val pageState by viewModel.pageState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(authChanged) {
        if (authChanged) {
            viewModel.refresh()
            onAuthChangedHandled()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pageState.selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 14.dp,
        ) {
            pageState.getTabs().forEach { tab ->
                Tab(
                    selected = pageState.selectedTab.ordinal == tab.ordinal,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = stringResource(tab.nameResId),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                )
            }
        }
        val currentState = pageState.getCurrentTabState()
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = {
                viewModel.refresh()
                coroutineScope.launch { pullToRefreshState.animateToHidden() }
            },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
        ) {
            when (currentState) {
                TabState.None -> LoadingTab()

                TabState.Loading -> LoadingTab()

                is TabState.Error -> {
                    ErrorTab(
                        errorType = currentState.type,
                        onLoginClicked = onLoginClicked,
                        onRetryClicked = viewModel::refresh,
                    )
                }

                is TabState.Content -> {
                    TemplatesGrid(
                        currentState = currentState,
                        onTemplateSelected = onTemplateSelected,
                        { viewModel.loadDataForTab(pageState.selectedTab) },
                    ) { id ->
                        viewModel.onLikeToggle(id)
                    }
                }

                TabState.Empty -> NoContentTab()
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewTemplatesFeed() {
    TemplatesFeedScreen(
        authChanged = false,
        onAuthChangedHandled = {},
        onLoginClicked = {},
        onTemplateSelected = {},
    )
}
