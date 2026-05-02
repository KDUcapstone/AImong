package com.kduniv.aimong.feature.quiz.data

import com.google.gson.Gson
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.local.dao.QuizDao
import com.kduniv.aimong.core.local.entity.OfflineMissionQueueEntity
import com.kduniv.aimong.core.local.entity.QuizMetadataEntity
import com.kduniv.aimong.core.local.entity.QuizQuestionEntity
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.quiz.data.model.QuizAnswer
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitRequest
import com.kduniv.aimong.feature.quiz.domain.model.*
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import java.util.*
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val offlineDao: OfflineMissionQueueDao,
    private val quizDao: QuizDao,
    private val gson: Gson
) : QuizRepository {
    override suspend fun getQuestions(missionId: String): Result<QuizQuestions> {
        return try {
            val response = apiService.getQuestions(missionId)
            if (response.success) {
                val data = response.data
                val questions = data.questions.map {
                    Question(
                        id = it.id,
                        type = QuestionType.valueOf(it.type),
                        question = it.question,
                        options = it.options
                    )
                }
                
                // 로컬 캐시 저장
                val metadata = QuizMetadataEntity(
                    missionId = data.missionId,
                    missionTitle = data.missionTitle,
                    isReview = data.isReview,
                    quizAttemptId = data.quizAttemptId,
                    expiresAt = data.expiresAt
                )
                val questionEntities = data.questions.map {
                    QuizQuestionEntity(
                        id = it.id,
                        missionId = data.missionId,
                        type = it.type,
                        question = it.question,
                        optionsJson = it.options?.let { opt -> gson.toJson(opt) }
                    )
                }
                quizDao.saveQuiz(metadata, questionEntities)

                Result.success(
                    QuizQuestions(
                        missionId = data.missionId,
                        missionTitle = data.missionTitle,
                        isReview = data.isReview,
                        quizAttemptId = data.quizAttemptId,
                        expiresAt = data.expiresAt,
                        questions = questions
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 캐시 확인
            val cachedMetadata = quizDao.getQuizMetadata(missionId)
            val cachedQuestions = quizDao.getQuizQuestions(missionId)
            
            if (cachedMetadata != null && cachedQuestions.isNotEmpty()) {
                val questions = cachedQuestions.map {
                    Question(
                        id = it.id,
                        type = QuestionType.valueOf(it.type),
                        question = it.question,
                        options = it.optionsJson?.let { json ->
                            gson.fromJson<List<String>>(json, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                        }
                    )
                }
                Result.success(
                    QuizQuestions(
                        missionId = cachedMetadata.missionId,
                        missionTitle = cachedMetadata.missionTitle,
                        isReview = cachedMetadata.isReview,
                        quizAttemptId = cachedMetadata.quizAttemptId,
                        expiresAt = cachedMetadata.expiresAt,
                        questions = questions
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun submitQuiz(
        missionId: String,
        quizAttemptId: String,
        answers: Map<String, String>
    ): Result<QuizResult> {
        val quizAnswers = answers.map { QuizAnswer(it.key, it.value) }
        val request = QuizSubmitRequest(quizAttemptId, quizAnswers)
        
        return try {
            val response = apiService.submitQuiz(missionId, request)
            
            if (response.success) {
                val data = response.data
                Result.success(
                    QuizResult(
                        score = data.score,
                        total = data.total,
                        isPassed = data.isPassed,
                        isPerfect = data.isPerfect,
                        xpEarned = data.xpEarned,
                        bonusXp = data.bonusXp,
                        bonusReason = data.bonusReason,
                        petEvolved = data.petEvolved,
                        streakDays = data.streakDays,
                        rewards = data.rewards.map {
                            QuizReward(
                                type = it.type,
                                ticketType = it.ticketType,
                                count = it.count,
                                reason = it.reason
                            )
                        },
                        results = data.results.map {
                            QuestionResult(
                                questionId = it.questionId,
                                isCorrect = it.isCorrect,
                                explanation = it.explanation
                            )
                        },
                        currentLevel = data.currentLevel ?: 1,
                        currentXp = data.currentXp ?: 0,
                        nextLevelXp = data.nextLevelXp ?: 100
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "서버 오류"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 오프라인 큐에 저장
            offlineDao.insertMission(
                OfflineMissionQueueEntity(
                    idempotencyKey = UUID.randomUUID().toString(),
                    missionId = missionId,
                    quizAttemptId = quizAttemptId,
                    answersJson = gson.toJson(quizAnswers),
                    attemptDate = System.currentTimeMillis()
                )
            )
            Result.failure(Exception("네트워크가 불안정하여 결과가 저장되었습니다. 연결 시 자동 동기화됩니다."))
        }
    }

    override suspend fun syncOfflineMissions(): Result<Unit> {
        return try {
            val unsynced = offlineDao.getUnsyncedMissions()
            for (mission in unsynced) {
                val answers = gson.fromJson<List<QuizAnswer>>(
                    mission.answersJson,
                    object : com.google.gson.reflect.TypeToken<List<QuizAnswer>>() {}.type
                )
                val request = QuizSubmitRequest(mission.quizAttemptId, answers)
                val response = apiService.submitQuiz(mission.missionId, request)
                if (response.success) {
                    offlineDao.markAsSynced(mission.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
