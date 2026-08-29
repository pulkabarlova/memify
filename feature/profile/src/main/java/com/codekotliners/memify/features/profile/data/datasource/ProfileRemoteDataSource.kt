package com.codekotliners.memify.features.profile.data.datasource

import android.content.Context
import androidx.core.net.toUri
import com.codekotliners.memify.core.common.Response
import com.codekotliners.memify.core.network.api.ApiConfig
import com.codekotliners.memify.core.network.api.authorizedRequest
import com.codekotliners.memify.core.network.models.PostDto
import com.codekotliners.memify.core.network.models.UserDto
import com.codekotliners.memify.core.prefs.TokenStore
import com.codekotliners.memify.core.repositories.likes.LikesRepository
import com.codekotliners.memify.core.repositories.user.UserRepository
import com.codekotliners.memify.features.profile.data.model.ProfileAccountData
import com.codekotliners.memify.features.profile.data.model.ProfileMemeData
import com.vk.id.VKID
import com.vk.id.VKIDUser
import com.vk.id.refreshuser.VKIDGetUserCallback
import com.vk.id.refreshuser.VKIDGetUserFail
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ProfileRemoteDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val tokenStore: TokenStore,
    private val likesRepository: LikesRepository,
    private val userRepository: UserRepository,
) {
    suspend fun getAccount(): ProfileAccountData {
        val user = loadCurrentUser()
        val displayName =
            user
                ?.username
                ?.takeIf { name -> name.isNotBlank() }
                ?: loadVkDisplayName().orEmpty()

        return ProfileAccountData(
            displayName = displayName,
            avatarUrl = user?.photoUrl,
        )
    }

    suspend fun getLikedMemes(): List<ProfileMemeData> =
        likesRepository.getLikedPosts().map { post -> post.toProfileMemeData() }

    suspend fun getCreatedMemes(): List<ProfileMemeData> {
        val userId = tokenStore.getUserId() ?: return emptyList()
        val posts: List<PostDto> =
            httpClient.authorizedRequest(tokenStore) {
                method = HttpMethod.Get
                url(ApiConfig.baseUrl + "posts?limit=$CREATED_POSTS_LIMIT")
            }

        return posts
            .asSequence()
            .filter { post -> post.authorId == userId }
            .map { post -> post.toProfileMemeData() }
            .toList()
    }

    suspend fun updateAvatar(imageUri: String): String {
        val uri = imageUri.toUri()
        val bytes =
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
            } ?: throw IllegalStateException("Can't read profile image")
        val contentType = context.contentResolver.getType(uri) ?: DEFAULT_IMAGE_CONTENT_TYPE

        val uploadResponse: Map<String, String?> =
            httpClient.authorizedRequest(tokenStore) {
                method = HttpMethod.Post
                url(ApiConfig.baseUrl + "upload")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"profile.jpg\"")
                                },
                            )
                        },
                    ),
                )
            }
        val downloadUrl =
            uploadResponse[UPLOAD_URL_KEY]
                ?: throw IllegalStateException("Profile image upload returned no URL")

        when (val response = userRepository.updateProfilePhoto(downloadUrl)) {
            is Response.Success -> {
                if (!response.data) {
                    throw IllegalStateException("Profile image URL was not saved")
                }
            }

            is Response.Failure -> throw response.error
            Response.Loading -> throw IllegalStateException("Unexpected loading state")
        }

        return downloadUrl
    }

    private suspend fun loadCurrentUser(): UserDto? =
        try {
            httpClient.authorizedRequest(tokenStore) {
                method = HttpMethod.Get
                url(ApiConfig.baseUrl + "users/me")
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }

    private suspend fun loadVkDisplayName(): String? {
        val result = CompletableDeferred<String?>()
        VKID.instance.getUserData(
            object : VKIDGetUserCallback {
                override fun onSuccess(user: VKIDUser) {
                    result.complete(user.firstName.takeIf { name -> name.isNotBlank() })
                }

                override fun onFail(fail: VKIDGetUserFail) {
                    result.complete(null)
                }
            },
        )
        return result.await()
    }

    private fun PostDto.toProfileMemeData(): ProfileMemeData =
        ProfileMemeData(
            id = id,
            imageUrl = imageUrl,
            width = width,
            height = height,
        )

    private companion object {
        const val UPLOAD_URL_KEY = "url"
        const val DEFAULT_IMAGE_CONTENT_TYPE = "image/*"
        const val CREATED_POSTS_LIMIT = 1_000
    }
}
