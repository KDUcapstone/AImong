package com.kduniv.aimong.feature.mission.data

import com.kduniv.aimong.core.local.dao.MissionDao
import com.kduniv.aimong.core.local.entity.MissionEntity
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.feature.mission.domain.model.Question
import com.kduniv.aimong.feature.mission.domain.model.QuizResult
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MissionRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val missionDao: MissionDao
) : MissionRepository {

    override fun getMissionsFlow(): Flow<List<Mission>> {
        return missionDao.getMissions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshMissions(): Result<MissionProgress> {
        return try {
            val response = apiService.getMissions()
            if (response.success) {
                val missionEntities = response.data.missions.map {
                    MissionEntity(
                        id = it.id,
                        stage = it.stage,
                        title = it.title,
                        description = it.description,
                        isUnlocked = it.isUnlocked,
                        isCompleted = it.isCompleted,
                        completedAt = it.completedAt,
                        isReviewable = it.isReviewable
                    )
                }
                missionDao.insertMissions(missionEntities)
                
                val progress = MissionProgress(
                    stage1Completed = response.data.stageProgress.stage1Completed,
                    stage2Completed = response.data.stageProgress.stage2Completed,
                    stage3Completed = response.data.stageProgress.stage3Completed
                )
                Result.success(progress)
            } else {
                Result.failure(Exception("미션 데이터를 가져오는데 실패했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuestions(missionId: String): Result<List<Question>> {
        // TODO: 실제 API 연동 로직 구현 예정 (UI 작업 우선)
        return Result.success(emptyList())
    }

    override suspend fun submitQuiz(missionId: String, answers: List<Int>): Result<QuizResult> {
        // TODO: 실제 API 연동 로직 구현 예정 (UI 작업 우선)
        return Result.success(QuizResult(0, 0, false, 0, 0))
    }

    private fun MissionEntity.toDomain() = Mission(
        id = id,
        stage = stage,
        title = title,
        description = description,
        isUnlocked = isUnlocked,
        isCompleted = isCompleted,
        completedAt = completedAt,
        isReviewable = isReviewable
    )
}
