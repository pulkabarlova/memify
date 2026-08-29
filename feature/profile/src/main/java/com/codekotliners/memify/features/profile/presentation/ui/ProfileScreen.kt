package com.codekotliners.memify.features.profile.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codekotliners.memify.core.theme.MemifyTheme
import com.codekotliners.memify.core.ui.components.AppScaffold
import com.codekotliners.memify.features.profile.R
import com.codekotliners.memify.features.profile.presentation.model.ProfileAccountUiModel
import com.codekotliners.memify.features.profile.presentation.model.ProfileAction
import com.codekotliners.memify.features.profile.presentation.model.ProfileMessage
import com.codekotliners.memify.features.profile.presentation.model.ProfileTab
import com.codekotliners.memify.features.profile.presentation.model.ProfileUiState
import com.codekotliners.memify.features.profile.presentation.ui.components.CreatedMemeItem
import com.codekotliners.memify.features.profile.presentation.ui.components.EmptyProfileSection
import com.codekotliners.memify.features.profile.presentation.ui.components.LikedMemeItem
import com.codekotliners.memify.features.profile.presentation.ui.components.ProfileFloatingActionButton
import com.codekotliners.memify.features.profile.presentation.ui.components.ProfileSummaryCard
import com.codekotliners.memify.features.profile.presentation.ui.components.ProfileTabs
import com.codekotliners.memify.features.profile.presentation.ui.components.ProfileTopBar
import com.codekotliners.memify.features.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    refreshRequested: Boolean,
    onRefreshHandled: () -> Unit,
    onSettingsClick: (isAuthenticated: Boolean, displayName: String, avatarUrl: String?) -> Unit,
    onLoginClick: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedScrollState = rememberLazyStaggeredGridState()
    val likedScrollState = rememberLazyStaggeredGridState()
    val activeScrollState = state.activeScrollState(savedScrollState, likedScrollState)
    val showScrollToTop by
        remember(activeScrollState) {
            derivedStateOf {
                activeScrollState.firstVisibleItemIndex > LAST_PROFILE_HEADER_INDEX ||
                    activeScrollState.firstVisibleItemScrollOffset > SCROLL_TO_TOP_THRESHOLD
            }
        }
    val coroutineScope = rememberCoroutineScope()
    val messageText =
        when (state.message) {
            ProfileMessage.PROFILE_LOAD_FAILED -> stringResource(R.string.profile_load_failed)
            ProfileMessage.CREATED_MEMES_LOAD_FAILED -> stringResource(R.string.created_memes_load_failed)
            ProfileMessage.LIKED_MEMES_LOAD_FAILED -> stringResource(R.string.liked_memes_load_failed)
            ProfileMessage.AVATAR_UPDATE_FAILED -> stringResource(R.string.avatar_update_failed)
            null -> null
        }

    LaunchedEffect(Unit) {
        if (!refreshRequested) {
            viewModel.onAction(ProfileAction.Refresh)
        }
    }

    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            onRefreshHandled()
            viewModel.onAction(ProfileAction.Refresh)
        }
    }

    LaunchedEffect(state.message, messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.onAction(ProfileAction.MessageShown)
        }
    }

    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ProfileTopBar(
                enabled = state.account !is ProfileAccountUiModel.Loading,
                onSettingsClick = {
                    onSettingsClick(
                        state.isLoggedIn,
                        state.displayName,
                        state.avatarUrl,
                    )
                },
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ProfileFloatingActionButton(
                visible = showScrollToTop,
                onClick = {
                    coroutineScope.launch {
                        activeScrollState.animateScrollToItem(0)
                    }
                },
            )
        },
    ) { innerPadding ->
        ProfileContent(
            state = state,
            savedScrollState = savedScrollState,
            likedScrollState = likedScrollState,
            onAction = viewModel::onAction,
            onLoginClick = onLoginClick,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    savedScrollState: LazyStaggeredGridState,
    likedScrollState: LazyStaggeredGridState,
    onAction: (ProfileAction) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTab = state.visibleSelectedTab()
    val activeScrollState = state.activeScrollState(savedScrollState, likedScrollState)

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = activeScrollState,
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        item(
            key = PROFILE_HEADER_KEY,
            span = StaggeredGridItemSpan.FullLine,
        ) {
            ProfileSummaryCard(
                state = state,
                onLoginClick = onLoginClick,
                onAvatarSelected = { imageUri ->
                    onAction(ProfileAction.AvatarSelected(imageUri))
                },
            )
        }

        item(
            key = PROFILE_TABS_KEY,
            span = StaggeredGridItemSpan.FullLine,
        ) {
            ProfileTabs(
                state = state,
                onTabSelected = { tab -> onAction(ProfileAction.TabSelected(tab)) },
            )
        }

        when (selectedTab) {
            ProfileTab.CREATED -> {
                if (state.createdMemes.isEmpty()) {
                    item(
                        key = EMPTY_CREATED_KEY,
                        span = StaggeredGridItemSpan.FullLine,
                    ) {
                        EmptyProfileSection(
                            icon = Icons.Default.Add,
                            title = stringResource(R.string.empty_created_title),
                            description = stringResource(R.string.empty_created_description),
                        )
                    }
                } else {
                    items(
                        items = state.createdMemes,
                        key = { meme -> "created_${meme.id}" },
                    ) { meme ->
                        CreatedMemeItem(meme)
                    }
                }
            }

            ProfileTab.LIKED -> {
                if (state.likedMemes.isEmpty()) {
                    item(
                        key = EMPTY_LIKED_KEY,
                        span = StaggeredGridItemSpan.FullLine,
                    ) {
                        EmptyProfileSection(
                            icon = Icons.Default.Favorite,
                            title = stringResource(R.string.empty_liked_title),
                            description = stringResource(R.string.empty_liked_description),
                        )
                    }
                } else {
                    items(
                        items = state.likedMemes,
                        key = { meme -> "liked_${meme.id}" },
                    ) { meme ->
                        LikedMemeItem(meme)
                    }
                }
            }
        }
    }
}

private fun ProfileUiState.visibleSelectedTab(): ProfileTab =
    if (isLoggedIn) selectedTab else ProfileTab.CREATED

private fun ProfileUiState.activeScrollState(
    savedScrollState: LazyStaggeredGridState,
    likedScrollState: LazyStaggeredGridState,
): LazyStaggeredGridState =
    if (visibleSelectedTab() == ProfileTab.LIKED) likedScrollState else savedScrollState

@Preview(name = "Profile light", showSystemUi = true)
@Preview(name = "Profile dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    MemifyTheme {
        Scaffold(
            topBar = { ProfileTopBar(onSettingsClick = {}) },
            contentWindowInsets = WindowInsets(0),
        ) { innerPadding ->
            ProfileContent(
                state =
                    ProfileUiState(
                        isLoading = false,
                        account =
                            ProfileAccountUiModel.Authenticated(
                                displayName = "MemeMaker2011",
                                avatarUrl = null,
                            ),
                    ),
                savedScrollState = rememberLazyStaggeredGridState(),
                likedScrollState = rememberLazyStaggeredGridState(),
                onAction = {},
                onLoginClick = {},
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        }
    }
}

private const val LAST_PROFILE_HEADER_INDEX = 1
private const val SCROLL_TO_TOP_THRESHOLD = 240
private const val PROFILE_HEADER_KEY = "profile_header"
private const val PROFILE_TABS_KEY = "profile_tabs"
private const val EMPTY_CREATED_KEY = "empty_created"
private const val EMPTY_LIKED_KEY = "empty_liked"
