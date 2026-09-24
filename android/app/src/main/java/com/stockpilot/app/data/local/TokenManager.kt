package com.stockpilot.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            "stockpilot_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("stockpilot_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_BASE_URL = "base_url"
        
        // Default backend address for local emulator / network testing
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api/v1/"
    }

    fun saveAuth(token: String, userId: Int, role: String, name: String, email: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        if (!cleanUrl.contains("/api/v1/")) {
            if (cleanUrl.endsWith("/api/v1")) {
                cleanUrl += "/"
            } else {
                cleanUrl += "api/v1/"
            }
        }
        prefs.edit().putString(KEY_BASE_URL, cleanUrl).apply()
    }

    fun clearAuth() {
        val currentBaseUrl = getBaseUrl()
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
        setBaseUrl(currentBaseUrl)
    }

    fun isLoggedIn(): Boolean = !getToken().isNull_Blank()

    private fun String?.isNull_Blank(): Boolean = this == null || this.trim().isEmpty()
}
