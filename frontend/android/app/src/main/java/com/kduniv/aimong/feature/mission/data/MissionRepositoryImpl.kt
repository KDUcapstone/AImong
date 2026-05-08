package com.kduniv.aimong.feature.mission.data

import com.kduniv.aimong.core.local.entity.MissionEntity
import com.kduniv.aimong.core.local.dao.MissionSetDao
import com.kduniv.aimong.core.local.entity.MissionSetEntity
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.mission.data.model.MissionSetDto
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
    private val missionSetDao: MissionSetDao
) : MissionRepository {

    override fun getMissionsFlow(): Flow<List<Mission>> {
        return missionSetDao.getMissionSets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshMissions(): Result<MissionProgress> {
        return try {
            val response = apiService.getMissions()
            if (response.success) {
                val sets = response.data.levels
                    .flatMap { level -> level.stages }
                    .flatMap { stage -> stage.sets }

                val entities = sets.map { it.toEntity() }
                missionSetDao.insertMissionSets(entities)

                val p = response.data.progress
                Result.success(
                    MissionProgress(
                        completedSetCount = p?.completedSetCount ?: sets.count { it.isCompleted },
                        totalSetCount = p?.totalSetCount ?: sets.size,
                        currentLevelNo = p?.currentLevelNo ?: 1
                    )
                )
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

    private fun MissionSetEntity.toDomain() = Mission(
        setId = setId,
        missionId = missionId,
        missionCode = missionCode,
        levelNo = levelNo,
        stage = stage,
        difficulty = difficulty,
        title = title,
        description = description,
        isUnlocked = isUnlocked,
        isCompleted = isCompleted,
        completedAt = completedAt,
        isReviewable = isReviewable
    )

    private fun MissionSetDto.toEntity() = MissionSetEntity(
        setId = setId,
        missionId = missionId,
        missionCode = missionCode,
        levelNo = levelNo,
        stage = stage,
        difficulty = difficulty,
        title = title,
        description = description,
        isUnlocked = isUnlocked,
        isCompleted = isCompleted,
        completedAt = completedAt,
        isReviewable = isReviewable
    )
}
