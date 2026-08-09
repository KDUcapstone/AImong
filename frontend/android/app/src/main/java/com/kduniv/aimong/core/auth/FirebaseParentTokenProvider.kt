package com.kduniv.aimong.core.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseParentTokenProvider @Inject constructor() {

    suspend fun getIdTokenOrNull(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return user.getIdToken(false).await().token?.trim()?.takeIf { it.isNotEmpty() }
    }
}
