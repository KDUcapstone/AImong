package com.aimong.android.core.network

import retrofit2.http.*

// TODO: Retrofit 인터페이스 — 전체 API 엔드포인트 정의
interface AimongApiService {
    // AUTH
    // @POST("parent/register") suspend fun registerParent(...)
    // @POST("child/login") suspend fun loginChild(...)

    // MISSION
    // @GET("missions/{id}/questions") suspend fun getQuestions(...)
    // @POST("missions/{id}/submit") suspend fun submitAnswers(...)

    // PET
    // @GET("pet") suspend fun getPet(...)

    // GACHA
    // @POST("gacha/pull") suspend fun pullGacha(...)

    // STREAK
    // @GET("streak") suspend fun getStreak(...)

    // QUEST
    // @GET("quests/daily") suspend fun getDailyQuests(...)

    // CHAT
    // @POST("chat/send") suspend fun sendChat(...)

    // PARENT
    // @GET("parent/child/{id}/summary") suspend fun getChildSummary(...)
}
