package com.kduniv.aimong.core.network

import android.content.Context
import android.content.Intent
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.core.local.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        val token = runBlocking { sessionManager.authToken.first() }
        val role = runBlocking { sessionManager.userRole.first() }
        
        val requestBuilder = original.newBuilder()
        
        if (original.header("Authorization") == null && token?.isNotBlank() == true) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        // 401 또는 403 에러 처리 (특히 CHILD 권한일 때 세션 만료로 간주)
        if ((response.code == 401 || response.code == 403) && role == "CHILD") {
            runBlocking { sessionManager.clearSession() }
            
            // 로그인 화면으로 튕겨냄
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            context.startActivity(intent)
        }
        
        return response
    }
}
