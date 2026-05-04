package com.kduniv.aimong.core.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_SESSION_VERSION = intPreferencesKey("session_version")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_PARENT_CHILDREN_JSON = stringPreferencesKey("parent_children_json")
    }

    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]
    }

    val sessionVersion: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SESSION_VERSION] ?: 1
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN]
    }

    /** GET /parent/children 캐시(JSON 배열). 재설치 전까지 복구용. */
    val parentChildrenJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PARENT_CHILDREN_JSON]
    }

    suspend fun saveParentChildrenJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PARENT_CHILDREN_JSON] = json
        }
    }

    suspend fun saveSession(role: String, version: Int, token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ROLE] = role
            preferences[KEY_SESSION_VERSION] = version
            preferences[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
