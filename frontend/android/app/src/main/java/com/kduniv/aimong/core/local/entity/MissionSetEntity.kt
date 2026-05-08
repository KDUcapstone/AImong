package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_sets")
data class MissionSetEntity(
    @PrimaryKey val setId: String,
    val missionId: String,
    val missionCode: String,
    val levelNo: Int,
    val stage: Int,
    val difficulty: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val completedAt: String?,
    val isReviewable: Boolean
)

