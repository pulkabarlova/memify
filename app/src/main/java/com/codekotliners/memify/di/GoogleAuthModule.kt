package com.codekotliners.memify.di

import android.content.Context
import com.codekotliners.memify.R
import com.codekotliners.memify.features.auth.di.GoogleWebClientId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Provides the Google Sign-In OAuth web client id to :feature:auth.
 *
 * The app owns `R.string.default_web_client_id`; the feature receives the value
 * through a qualifier because Android R classes are module-scoped.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleAuthModule {
    @Provides
    @GoogleWebClientId
    fun provideGoogleWebClientId(@ApplicationContext context: Context): String =
        context.getString(R.string.default_web_client_id)
}
