package com.codekotliners.memify.features.auth.di

import javax.inject.Qualifier

/**
 * Qualifier for the OAuth web client id used for Google Sign-In.
 *
 * The actual value belongs to the `:app` module as `R.string.default_web_client_id`.
 * This feature cannot read the app's module-scoped R class, so the app provides the
 * value through this qualifier.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleWebClientId
