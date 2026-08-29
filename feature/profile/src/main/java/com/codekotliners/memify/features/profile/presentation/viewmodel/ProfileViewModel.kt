package com.codekotliners.memify.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekotliners.memify.features.profile.domain.repository.ProfileRepository
import com.codekotliners.memify.features.profile.domain.usecase.LoadProfileUseCase
import com.codekotliners.memify.features.profile.presentation.model.ProfileAccountUiModel
import com.codekotliners.memify.features.profile.presentation.model.ProfileAction
import com.codekotliners.memify.features.profile.presentation.model.ProfileMessage
import com.codekotliners.memify.features.profile.presentation.model.ProfileMemeUiModel
import com.codekotliners.memify.features.profile.presentation.model.ProfileTab
import com.codekotliners.memify.features.profile.presentation.model.ProfileUiState
import com.codekotliners.memify.features.profile.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val loadProfileUseCase: LoadProfileUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var localCreatedMemes = emptyList<ProfileMemeUiModel>()

    init {
        observeLocalMemes()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Refresh -> refresh()
            ProfileAction.MessageShown -> _uiState.update { state -> state.copy(message = null) }
            is ProfileAction.AvatarSelected -> updateAvatar(action.imageUri)
            is ProfileAction.TabSelected -> selectTab(action.tab)
        }
    }

    private fun observeLocalMemes() {
        repository
            .observeLocalMemes()
            .onEach { memes ->
                localCreatedMemes = memes.map { meme -> meme.toUiModel() }
                _uiState.update { state ->
                    if (state.account is ProfileAccountUiModel.Authenticated) {
                        state
                    } else {
                        state.copy(createdMemes = localCreatedMemes)
                    }
                }
            }.catch { exception ->
                if (exception is CancellationException) throw exception
                _uiState.update { state -> state.copy(message = ProfileMessage.PROFILE_LOAD_FAILED) }
            }.launchIn(viewModelScope)
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                _uiState.update { state -> state.copy(isLoading = true) }

                try {
                    val snapshot = loadProfileUseCase()
                    val loadedAccount = snapshot.account.toUiModel()

                    _uiState.update { state ->
                        val currentAccount = state.account as? ProfileAccountUiModel.Authenticated
                        val account =
                            if (
                                state.isAvatarUpdating &&
                                currentAccount != null &&
                                loadedAccount is ProfileAccountUiModel.Authenticated
                            ) {
                                loadedAccount.copy(avatarUrl = currentAccount.avatarUrl)
                            } else {
                                loadedAccount
                            }

                        state.copy(
                            isLoading = false,
                            account = account,
                            selectedTab =
                                if (account is ProfileAccountUiModel.Guest) {
                                    ProfileTab.CREATED
                                } else {
                                    state.selectedTab
                                },
                            createdMemes =
                                when {
                                    account is ProfileAccountUiModel.Guest -> localCreatedMemes
                                    snapshot.createdMemesLoadFailed -> state.createdMemes
                                    else -> snapshot.createdMemes.map { meme -> meme.toUiModel() }
                                },
                            likedMemes =
                                when {
                                    account is ProfileAccountUiModel.Guest -> emptyList()
                                    snapshot.likedMemesLoadFailed -> state.likedMemes
                                    else -> snapshot.likedMemes.map { meme -> meme.toUiModel() }
                                },
                            message =
                                when {
                                    snapshot.createdMemesLoadFailed -> ProfileMessage.CREATED_MEMES_LOAD_FAILED
                                    snapshot.likedMemesLoadFailed -> ProfileMessage.LIKED_MEMES_LOAD_FAILED
                                    else -> state.message
                                },
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            account =
                                if (state.account is ProfileAccountUiModel.Loading) {
                                    ProfileAccountUiModel.Guest
                                } else {
                                    state.account
                                },
                            message = ProfileMessage.PROFILE_LOAD_FAILED,
                        )
                    }
                }
            }
    }

    private fun selectTab(tab: ProfileTab) {
        _uiState.update { state ->
            if (tab == ProfileTab.LIKED && !state.isLoggedIn) {
                state
            } else {
                state.copy(selectedTab = tab)
            }
        }
    }

    private fun updateAvatar(imageUri: String) {
        val currentState = _uiState.value
        val currentAccount = currentState.account as? ProfileAccountUiModel.Authenticated ?: return
        if (currentState.isAvatarUpdating) return

        _uiState.update { state ->
            state.copy(
                account = currentAccount.copy(avatarUrl = imageUri),
                isAvatarUpdating = true,
            )
        }

        viewModelScope.launch {
            try {
                val remoteUrl = repository.updateAvatar(imageUri)
                _uiState.update { state ->
                    val account = state.account as? ProfileAccountUiModel.Authenticated
                    state.copy(
                        account = account?.copy(avatarUrl = remoteUrl) ?: state.account,
                        isAvatarUpdating = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { state ->
                    val account = state.account as? ProfileAccountUiModel.Authenticated
                    state.copy(
                        account = account?.copy(avatarUrl = currentAccount.avatarUrl) ?: state.account,
                        isAvatarUpdating = false,
                        message = ProfileMessage.AVATAR_UPDATE_FAILED,
                    )
                }
            }
        }
    }
}
