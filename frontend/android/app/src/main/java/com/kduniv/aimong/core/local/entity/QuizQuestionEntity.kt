package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val missionId: String,
    val type: String,
    val question: String,
    val optionsJson: String? // List<String>을 JSON으로 저장
)

@Entity(tableName = "quiz_metadata")
data class QuizMetadataEntity(
    @PrimaryKey val missionId: String,
    val missionTitle: String,
    val isReview: Boolean,
    val quizAttemptId: String,
    val expiresAt: String,
    /** 서버 questionCount (스펙상 10) */
    val questionCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
