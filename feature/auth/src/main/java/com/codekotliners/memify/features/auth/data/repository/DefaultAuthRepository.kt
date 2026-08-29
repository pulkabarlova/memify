package com.codekotliners.memify.features.auth.data.repository

import com.codekotliners.memify.core.network.api.ApiException
import com.codekotliners.memify.features.auth.data.datasource.AuthLocalDataSource
import com.codekotliners.memify.features.auth.data.datasource.AuthRemoteDataSource
import com.codekotliners.memify.features.auth.data.model.AuthSessionData
import com.codekotliners.memify.features.auth.domain.model.AuthFailure
import com.codekotliners.memify.features.auth.domain.model.AuthResult
import com.codekotliners.memify.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultAuthRepository @Inject constructor(
    private val localDataSource: AuthLocalDataSource,
    private val remoteDataSource: AuthRemoteDataSource,
) : AuthRepository {
    override fun isAuthenticated(): Boolean = localDataSource.isAuthenticated()

    override suspend fun signIn(email: String, password: String): AuthResult =
        authenticate(AuthRequest.SignIn) {
            remoteDataSource.signIn(email = email, password = password)
        }

    override suspend fun register(name: String, email: String, password: String): AuthResult =
        authenticate(AuthRequest.Registration) {
            remoteDataSource.register(name = name, email = email, password = password)
        }

    override suspend fun signInWithGoogle(idToken: String): AuthResult =
        authenticate(AuthRequest.Google) {
            remoteDataSource.signInWithGoogle(idToken)
        }

    private suspend fun authenticate(
        request: AuthRequest,
        loadSession: suspend () -> AuthSessionData,
    ): AuthResult =
        try {
            localDataSource.saveSession(loadSession())
            AuthResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: IOException) {
            AuthResult.Failure(AuthFailure.Network)
        } catch (exception: ApiException) {
            AuthResult.Failure(exception.toFailure(request))
        } catch (_: Exception) {
            AuthResult.Failure(AuthFailure.Unknown)
        }
}

private enum class AuthRequest {
    SignIn,
    Registration,
    Google,
}

private fun ApiException.toFailure(request: AuthRequest): AuthFailure =
    when {
        statusCode == 408 || statusCode == 429 || statusCode >= 500 -> AuthFailure.ServiceUnavailable
        request == AuthRequest.SignIn && statusCode in setOf(400, 401, 403) -> AuthFailure.InvalidCredentials
        request == AuthRequest.Registration && statusCode == 409 -> AuthFailure.EmailAlreadyUsed
        request == AuthRequest.Google && statusCode in setOf(400, 401, 403) -> AuthFailure.GoogleRejected
        statusCode in 400..499 -> AuthFailure.RequestRejected
        else -> AuthFailure.Unknown
    }
