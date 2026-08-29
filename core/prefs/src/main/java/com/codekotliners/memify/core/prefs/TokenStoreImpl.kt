package com.codekotliners.memify.core.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE_NAME = "memify_secure_prefs"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"

/**
 * Токены шифруются на диске через EncryptedSharedPreferences (androidx.security-crypto) —
 * это замена тому, что раньше Firebase Auth SDK делал сам под капотом.
 */
@Singleton
class TokenStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {
    private val prefs by lazy { createEncryptedPreferencesWithRecovery() }

    private fun createEncryptedPreferencesWithRecovery(): SharedPreferences =
        try {
            createEncryptedPreferences()
        } catch (_: AEADBadTagException) {
            // The encrypted keyset cannot be recovered when SharedPreferences were restored
            // without their Android Keystore key. Drop only the invalid session and start fresh.
            context
                .getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            createEncryptedPreferences()
        }

    private fun createEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        prefs
            .edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    override fun saveAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun isLoggedIn(): Boolean = getAccessToken() != null
}
