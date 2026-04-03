package com.aimong.android.core.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO: DataStore — childSessionToken, childId 저장/조회
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        val KEY_CHILD_ID = stringPreferencesKey("child_id")
    }
    // TODO: suspend fun getToken(): String?
    // TODO: suspend fun saveToken(token: String)
    // TODO: suspend fun clearSession()
}
