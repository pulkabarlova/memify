package com.codekotliners.memify.features.profile.data.datasource

import com.codekotliners.memify.core.prefs.TokenStore
import com.codekotliners.memify.core.repositories.UriRepository
import com.codekotliners.memify.features.profile.data.model.ProfileMemeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ProfileLocalDataSource @Inject constructor(
    private val uriRepository: UriRepository,
    private val tokenStore: TokenStore,
) {
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun observeLocalMemes(): Flow<List<ProfileMemeData>> =
        uriRepository.getAllUris().map { entities ->
            entities.map { entity ->
                ProfileMemeData(
                    id = entity.id.toString(),
                    imageUrl = entity.uri,
                    width = 1,
                    height = 1,
                )
            }
        }
}
