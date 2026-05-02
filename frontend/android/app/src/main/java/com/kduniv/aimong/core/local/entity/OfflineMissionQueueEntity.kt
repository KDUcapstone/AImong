package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_mission_queue")
data class OfflineMissionQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idempotencyKey: String, // 중복 제출 방지
    val missionId: String,
    val score: Int,
    val attemptDate: Long,
    val isSync: Boolean = false
)
