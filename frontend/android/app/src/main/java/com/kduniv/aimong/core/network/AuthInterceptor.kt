package com.kduniv.aimong.core.network

import android.content.Context
import android.content.Intent
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.core.auth.FirebaseParentTokenProvider
import com.kduniv.aimong.core.local.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val firebaseParentTokenProvider: FirebaseParentTokenProvider,
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val (token, role) = runBlocking {
            sessionManager.authToken.first() to sessionManager.userRole.first()
        }

        val requestBuilder = original.newBuilder()

        if (original.header("Authorization") == null) {
            when {
                token?.isNotBlank() == true ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                role == "PARENT" -> {
                    val firebaseToken = runBlocking { firebaseParentTokenProvider.getIdTokenOrNull() }
                    if (!firebaseToken.isNullOrBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $firebaseToken")
                    }
                }
            }
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            runBlocking { sessionManager.clearSession() }
            if (role == "CHILD" || role == "PARENT") {
                navigateToLoginOnce()
            }
        }

        return response
    }

    private fun navigateToLoginOnce() {
        if (!loginRedirectInFlight.compareAndSet(false, true)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(MainActivity.EXTRA_IS_RESTART, true)
        }
        context.startActivity(intent)
    }

    companion object {
        private val loginRedirectInFlight = AtomicBoolean(false)

        fun resetLoginRedirectGate() {
            loginRedirectInFlight.set(false)
        }
    }
}
