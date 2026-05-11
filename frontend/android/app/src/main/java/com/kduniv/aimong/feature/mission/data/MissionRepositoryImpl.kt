package com.kduniv.aimong.feature.mission.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.dao.MissionChapterDao
import com.kduniv.aimong.core.local.entity.MissionChapterEntity
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.Question
import com.kduniv.aimong.feature.mission.domain.model.QuizResult
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private data class StarLevelSnapshot(
    val starLevel: Int,
    val label: String,
    val totalSetCount: Int,
    val completedSetCount: Int,
    val isPlayable: Boolean,
    val isReviewable: Boolean
)

class MissionRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val missionChapterDao: MissionChapterDao,
    private val gson: Gson
) : MissionRepository {

    override fun getMissionsFlow(): Flow<List<Mission>> {
        return missionChapterDao.getChapters().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshMissions(): Result<MissionProgress> {
        return try {
            val response = apiService.getMissions()
            if (response.success) {
                val data = response.data
                val chapters = data.stages.flatMap { stageDto ->
                    stageDto.missions.map { m ->
                        val stars = m.starLevels.map { s ->
                            StarLevelSnapshot(
                                starLevel = s.starLevel,
                                label = s.label,
                                totalSetCount = s.totalSetCount,
                                completedSetCount = s.completedSetCount,
                                isPlayable = s.isPlayable,
                                isReviewable = s.isReviewable
                            )
                        }
                        MissionChapterEntity(
                            missionId = m.missionId,
                            missionCode = m.missionCode,
                            stage = stageDto.stage,
                            title = m.title,
                            description = m.description,
                            isUnlocked = m.isUnlocked,
                            starLevelsJson = gson.toJson(stars)
                        )
                    }
                }
                missionChapterDao.clearChapters()
                missionChapterDao.insertChapters(chapters)

                val p = data.progress
                Result.success(
                    MissionProgress(
                        completedSetCount = p?.completedSetCount ?: 0,
                        totalSetCount = p?.totalSetCount ?: 0
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
        return Result.success(emptyList())
    }

    override suspend fun submitQuiz(missionId: String, answers: List<Int>): Result<QuizResult> {
        return Result.success(QuizResult(0, 0, false, 0, 0))
    }

    private fun MissionChapterEntity.toDomain(): Mission {
        val type = object : TypeToken<List<StarLevelSnapshot>>() {}.type
        val stars: List<StarLevelSnapshot> = try {
            gson.fromJson(starLevelsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return Mission(
            missionId = missionId,
            missionCode = missionCode,
            stage = stage,
            title = title,
            description = description,
            isUnlocked = isUnlocked,
            starLevels = stars.map {
                MissionStarLevel(
                    starLevel = it.starLevel,
                    label = it.label,
                    totalSetCount = it.totalSetCount,
                    completedSetCount = it.completedSetCount,
                    isPlayable = it.isPlayable,
                    isReviewable = it.isReviewable
                )
            }
        )
    }
}
