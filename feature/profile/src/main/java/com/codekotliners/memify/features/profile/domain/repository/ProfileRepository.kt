package com.codekotliners.memify.features.profile.domain.repository

import com.codekotliners.memify.features.profile.domain.model.ProfileAccount
import com.codekotliners.memify.features.profile.domain.model.ProfileMeme
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeLocalMemes(): Flow<List<ProfileMeme>>

    suspend fun getAccount(): ProfileAccount

    suspend fun getCreatedMemes(): List<ProfileMeme>

    suspend fun getLikedMemes(): List<ProfileMeme>

    suspend fun updateAvatar(imageUri: String): String
}
