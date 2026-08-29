package com.codekotliners.memify.features.auth.data.datasource

import com.codekotliners.memify.core.network.api.ApiConfig
import com.codekotliners.memify.core.network.api.ApiException
import com.codekotliners.memify.core.network.models.AuthResponseDto
import com.codekotliners.memify.core.network.models.ErrorResponseDto
import com.codekotliners.memify.core.network.models.GoogleAuthRequestDto
import com.codekotliners.memify.core.network.models.LoginRequestDto
import com.codekotliners.memify.core.network.models.RegisterRequestDto
import com.codekotliners.memify.features.auth.data.model.AuthSessionData
import com.codekotliners.memify.features.auth.data.model.toData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

internal class AuthRemoteDataSource @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun signIn(email: String, password: String): AuthSessionData =
        post<AuthResponseDto, LoginRequestDto>(
            path = "auth/login",
            body = LoginRequestDto(email = email, password = password),
        ).toData()

    suspend fun register(name: String, email: String, password: String): AuthSessionData =
        post<AuthResponseDto, RegisterRequestDto>(
            path = "auth/register",
            body = RegisterRequestDto(email = email, password = password, username = name),
        ).toData()

    suspend fun signInWithGoogle(idToken: String): AuthSessionData =
        post<AuthResponseDto, GoogleAuthRequestDto>(
            path = "auth/google",
            body = GoogleAuthRequestDto(idToken = idToken),
        ).toData()

    private suspend inline fun <reified ResponseBody, reified RequestBody> post(
        path: String,
        body: RequestBody,
    ): ResponseBody {
        val response =
            httpClient.request {
                method = HttpMethod.Post
                url(ApiConfig.baseUrl + path)
                contentType(ContentType.Application.Json)
                setBody(body)
            }

        if (!response.status.isSuccess()) {
            val message =
                try {
                    response.body<ErrorResponseDto>().error
                } catch (_: Exception) {
                    response.status.description
                }
            throw ApiException(response.status.value, message)
        }

        return response.body()
    }
}
