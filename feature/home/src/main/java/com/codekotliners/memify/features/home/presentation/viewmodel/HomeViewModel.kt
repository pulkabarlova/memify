package com.codekotliners.memify.features.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekotliners.memify.features.home.domain.model.HomeFeedResult
import com.codekotliners.memify.features.home.domain.model.HomeLikeUpdate
import com.codekotliners.memify.features.home.domain.model.ToggleHomePostLikeResult
import com.codekotliners.memify.features.home.domain.repository.HomeRepository
import com.codekotliners.memify.features.home.domain.usecase.ToggleHomePostLikeUseCase
import com.codekotliners.memify.features.home.presentation.model.HomeAction
import com.codekotliners.memify.features.home.presentation.model.HomeErrorUiModel
import com.codekotliners.memify.features.home.presentation.model.HomeFeedUiModel
import com.codekotliners.memify.features.home.presentation.model.HomeMessage
import com.codekotliners.memify.features.home.presentation.model.HomeNavigation
import com.codekotliners.memify.features.home.presentation.model.HomePostUiModel
import com.codekotliners.memify.features.home.presentation.model.HomeUiState
import com.codekotliners.memify.features.home.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val toggleHomePostLikeUseCase: ToggleHomePostLikeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var likeUpdateGeneration = 0L
    private val recentLikeUpdates = mutableMapOf<String, VersionedLikeUpdate>()

    init {
        loadFeed(isRefresh = false)
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> loadFeed(isRefresh = true)
            HomeAction.MessageShown -> _uiState.update { state -> state.copy(message = null) }
            HomeAction.NavigationHandled -> _uiState.update { state -> state.copy(navigation = null) }
            is HomeAction.LikeClicked -> toggleLike(action.postId)
        }
    }

    private fun loadFeed(isRefresh: Boolean) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                val hasContent = _uiState.value.feed is HomeFeedUiModel.Content
                val likeGenerationAtStart = likeUpdateGeneration
                _uiState.update { state ->
                    state.copy(
                        feed = if (hasContent) state.feed else HomeFeedUiModel.Loading,
                        isRefreshing = isRefresh && hasContent,
                    )
                }

                when (val result = repository.loadFeed()) {
                    is HomeFeedResult.Success -> {
                        val posts =
                            result.posts
                                .map { post -> post.toUiModel() }
                                .map { post ->
                                    recentLikeUpdates[post.id]
                                        ?.takeIf { update -> update.generation > likeGenerationAtStart }
                                        ?.let { update -> post.withLikeUpdate(update.value) }
                                        ?: post
                                }
                        recentLikeUpdates.entries.removeAll { (_, update) ->
                            update.generation <= likeGenerationAtStart
                        }
                        _uiState.update { state ->
                            state.copy(
                                feed =
                                    if (posts.isEmpty()) {
                                        HomeFeedUiModel.Empty
                                    } else {
                                        HomeFeedUiModel.Content(posts)
                                    },
                                isRefreshing = false,
                            )
                        }
                    }

                    HomeFeedResult.NetworkFailure -> handleLoadFailure(HomeErrorUiModel.Network, hasContent)
                    HomeFeedResult.UnknownFailure -> handleLoadFailure(HomeErrorUiModel.Unknown, hasContent)
                }
            }
    }

    private fun handleLoadFailure(
        error: HomeErrorUiModel,
        keepContent: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                feed = if (keepContent) state.feed else HomeFeedUiModel.Error(error),
                isRefreshing = false,
                message = if (keepContent) HomeMessage.FeedRefreshFailed else state.message,
            )
        }
    }

    private fun toggleLike(postId: String) {
        val currentState = _uiState.value
        if (postId in currentState.pendingLikePostIds) return

        val currentPost =
            (currentState.feed as? HomeFeedUiModel.Content)
                ?.posts
                ?.firstOrNull { post -> post.id == postId }
                ?: return
        val previousUpdate = currentPost.toLikeUpdate()
        val optimisticUpdate =
            previousUpdate.copy(
                isLiked = !previousUpdate.isLiked,
                likesCount =
                    if (previousUpdate.isLiked) {
                        (previousUpdate.likesCount - 1).coerceAtLeast(0)
                    } else {
                        previousUpdate.likesCount + 1
                    },
            )

        _uiState.update { state ->
            state.copy(pendingLikePostIds = state.pendingLikePostIds + postId)
        }
        applyLikeUpdate(optimisticUpdate)

        viewModelScope.launch {
            try {
                when (val result = toggleHomePostLikeUseCase(postId)) {
                    ToggleHomePostLikeResult.AuthenticationRequired -> {
                        applyLikeUpdate(previousUpdate)
                        _uiState.update { state -> state.copy(navigation = HomeNavigation.Auth) }
                    }

                    is ToggleHomePostLikeResult.Updated -> {
                        applyLikeUpdate(result.update)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                applyLikeUpdate(previousUpdate)
            } finally {
                _uiState.update { state ->
                    state.copy(pendingLikePostIds = state.pendingLikePostIds - postId)
                }
            }
        }
    }

    private fun applyLikeUpdate(update: HomeLikeUpdate) {
        likeUpdateGeneration += 1
        recentLikeUpdates[update.postId] =
            VersionedLikeUpdate(
                generation = likeUpdateGeneration,
                value = update,
            )
        _uiState.update { state ->
            state.copy(feed = state.feed.withLikeUpdate(update))
        }
    }
}

private fun HomeFeedUiModel.withLikeUpdate(
    update: HomeLikeUpdate,
): HomeFeedUiModel =
    if (this is HomeFeedUiModel.Content) {
        copy(
            posts =
                posts.map { post ->
                    if (post.id == update.postId) {
                        post.copy(
                            isLiked = update.isLiked,
                            likesCount = update.likesCount,
                        )
                    } else {
                        post
                    }
                },
        )
    } else {
        this
    }

private fun HomePostUiModel.withLikeUpdate(update: HomeLikeUpdate): HomePostUiModel =
    copy(
        isLiked = update.isLiked,
        likesCount = update.likesCount,
    )

private fun HomePostUiModel.toLikeUpdate(): HomeLikeUpdate =
    HomeLikeUpdate(
        postId = id,
        isLiked = isLiked,
        likesCount = likesCount,
    )

private data class VersionedLikeUpdate(
    val generation: Long,
    val value: HomeLikeUpdate,
)
