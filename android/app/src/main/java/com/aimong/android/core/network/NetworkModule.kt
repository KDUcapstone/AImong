package com.aimong.android.core.network

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// TODO: Hilt 모듈 — Retrofit, OkHttp 빈 제공
//       베이스 URL: BuildConfig.BASE_URL
//       AuthInterceptor 등록
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
