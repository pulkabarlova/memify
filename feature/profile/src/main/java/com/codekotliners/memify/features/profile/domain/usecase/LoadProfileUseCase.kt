package com.codekotliners.memify.features.profile.domain.usecase

import com.codekotliners.memify.features.profile.domain.model.ProfileAccount
import com.codekotliners.memify.features.profile.domain.model.ProfileMeme
import com.codekotliners.memify.features.profile.domain.model.ProfileSnapshot
import com.codekotliners.memify.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class LoadProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): ProfileSnapshot {
        val account = repository.getAccount()
        if (account is ProfileAccount.Guest) {
            return ProfileSnapshot(
                account = account,
                createdMemes = emptyList(),
                likedMemes = emptyList(),
            )
        }

        return supervisorScope {
            val createdMemes = async { loadMemes(repository::getCreatedMemes) }
            val likedMemes = async { loadMemes(repository::getLikedMemes) }
            val createdResult = createdMemes.await()
            val likedResult = likedMemes.await()

            ProfileSnapshot(
                account = account,
                createdMemes = createdResult.memes,
                likedMemes = likedResult.memes,
                createdMemesLoadFailed = createdResult.failed,
                likedMemesLoadFailed = likedResult.failed,
            )
        }
    }

    private suspend fun loadMemes(loader: suspend () -> List<ProfileMeme>): MemeLoadResult =
        try {
            MemeLoadResult(memes = loader())
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            MemeLoadResult(
                memes = emptyList(),
                failed = true,
            )
        }

    private data class MemeLoadResult(
        val memes: List<ProfileMeme>,
        val failed: Boolean = false,
    )
}
