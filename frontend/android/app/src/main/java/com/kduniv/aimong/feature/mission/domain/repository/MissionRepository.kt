package com.kduniv.aimong.feature.mission.domain.repository

import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.feature.mission.domain.model.Question
import com.kduniv.aimong.feature.mission.domain.model.QuizResult
import kotlinx.coroutines.flow.Flow

interface MissionRepository {
    fun getMissionsFlow(): Flow<List<Mission>>
    suspend fun refreshMissions(): Result<MissionProgress>
    suspend fun getQuestions(missionId: String): Result<List<Question>>
    suspend fun submitQuiz(missionId: String, answers: List<Int>): Result<QuizResult>
}
