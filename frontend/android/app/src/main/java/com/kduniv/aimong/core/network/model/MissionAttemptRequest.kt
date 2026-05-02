package com.kduniv.aimong.core.network.model

data class MissionAttemptRequest(
    val idempotencyKey: String,
    val missionId: String,
    val score: Int,
    val attemptDate: Long
)
