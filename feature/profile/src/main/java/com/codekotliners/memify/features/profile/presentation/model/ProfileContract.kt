package com.codekotliners.memify.features.profile.presentation.model

sealed interface ProfileAccountUiModel {
    data object Loading : ProfileAccountUiModel

    data object Guest : ProfileAccountUiModel

    data class Authenticated(
        val displayName: String,
        val avatarUrl: String?,
    ) : ProfileAccountUiModel
}

data class ProfileMemeUiModel(
    val id: String,
    val imageUrl: String,
    val aspectRatio: Float,
)

enum class ProfileTab {
    CREATED,
    LIKED,
}

enum class ProfileMessage {
    PROFILE_LOAD_FAILED,
    CREATED_MEMES_LOAD_FAILED,
    LIKED_MEMES_LOAD_FAILED,
    AVATAR_UPDATE_FAILED,
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val account: ProfileAccountUiModel = ProfileAccountUiModel.Loading,
    val selectedTab: ProfileTab = ProfileTab.CREATED,
    val createdMemes: List<ProfileMemeUiModel> = emptyList(),
    val likedMemes: List<ProfileMemeUiModel> = emptyList(),
    val isAvatarUpdating: Boolean = false,
    val message: ProfileMessage? = null,
) {
    val isLoggedIn: Boolean
        get() = account is ProfileAccountUiModel.Authenticated

    val displayName: String
        get() = (account as? ProfileAccountUiModel.Authenticated)?.displayName.orEmpty()

    val avatarUrl: String?
        get() = (account as? ProfileAccountUiModel.Authenticated)?.avatarUrl
}

sealed interface ProfileAction {
    data object Refresh : ProfileAction

    data class TabSelected(
        val tab: ProfileTab,
    ) : ProfileAction

    data class AvatarSelected(
        val imageUri: String,
    ) : ProfileAction

    data object MessageShown : ProfileAction
}
