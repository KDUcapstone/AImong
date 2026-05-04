package com.kduniv.aimong.feature.quiz.data

import com.google.gson.Gson
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.local.dao.QuizDao
import com.kduniv.aimong.core.local.entity.OfflineMissionQueueEntity
import com.kduniv.aimong.core.local.entity.QuizMetadataEntity
import com.kduniv.aimong.core.local.entity.QuizQuestionEntity
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizAnswer
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizReward
import com.kduniv.aimong.feature.quiz.domain.model.RemainingTickets
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import retrofit2.HttpException
import java.io.IOException
import java.util.*
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val offlineDao: OfflineMissionQueueDao,
    private val quizDao: QuizDao,
    private val gson: Gson
) : QuizRepository {

    override suspend fun getQuestions(missionId: String): kotlin.Result<QuizQuestions> {
        return try {
            val response = apiService.getQuestions(missionId)
            if (response.success) {
                val data = response.data
                val mapped = QuizSessionRules.mapQuestionResponses(data.questions).getOrElse {
                    return kotlin.Result.failure(it)
                }
                val quiz = QuizSessionRules.buildQuizQuestions(
                    missionId = data.missionId,
                    missionTitle = data.missionTitle,
                    isReview = data.isReview,
                    quizAttemptId = data.quizAttemptId,
                    questionCount = data.questionCount,
                    expiresAt = data.expiresAt,
                    questions = mapped
                ).getOrElse {
                    return kotlin.Result.failure(it)
                }

                persistQuizCache(data)

                kotlin.Result.success(quiz)
            } else {
                kotlin.Result.failure(
                    Exception(ApiErrorMapper.userMessageForApiError(response.error))
                )
            }
        } catch (e: HttpException) {
            fallbackCacheOr(missionId) {
                Exception(ApiErrorMapper.userMessageForHttpException(e))
            }
        } catch (e: Exception) {
            fallbackCacheOr(missionId) { e }
        }
    }

    private suspend fun persistQuizCache(data: QuizQuestionsResponse) {
        val metadata = QuizMetadataEntity(
            missionId = data.missionId,
            missionTitle = data.missionTitle,
            isReview = data.isReview,
            quizAttemptId = data.quizAttemptId,
            expiresAt = data.expiresAt,
            questionCount = data.questionCount
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
    }

    /** 네트워크 실패 시에만: TTL 유효하고 문항 10개 검증 통과할 때만 캐시 성공 */
    private suspend fun fallbackCacheOr(
        missionId: String,
        primaryException: () -> Exception
    ): kotlin.Result<QuizQuestions> {
        val cached = loadValidCachedQuiz(missionId)
        if (cached != null) return kotlin.Result.success(cached)
        return kotlin.Result.failure(primaryException())
    }

    private suspend fun loadValidCachedQuiz(missionId: String): QuizQuestions? {
        val meta = quizDao.getQuizMetadata(missionId) ?: return null
        val entities = quizDao.getQuizQuestions(missionId)
        if (entities.isEmpty()) return null

        if (QuizSessionRules.isSessionExpired(meta.expiresAt)) return null

        val questions = mutableListOf<Question>()
        for (entity in entities) {
            val type = QuizSessionRules.parseQuestionType(entity.type).getOrElse { return null }
            val opts = entity.optionsJson?.let { json ->
                gson.fromJson<List<String>>(
                    json,
                    object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                )
            }
            questions.add(
                Question(
                    id = entity.id,
                    type = type,
                    question = entity.question,
                    options = opts
                )
            )
        }

        return QuizSessionRules.buildQuizQuestions(
            missionId = meta.missionId,
            missionTitle = meta.missionTitle,
            isReview = meta.isReview,
            quizAttemptId = meta.quizAttemptId,
            questionCount = meta.questionCount,
            expiresAt = meta.expiresAt,
            questions = questions
        ).getOrNull()
    }

    override suspend fun submitQuiz(
        missionId: String,
        quizAttemptId: String,
        answers: Map<String, String>
    ): kotlin.Result<QuizResult> {
        val quizAnswers = answers.map { QuizAnswer(it.key, it.value) }
        val request = QuizSubmitRequest(quizAttemptId, quizAnswers)

        return try {
            val response = apiService.submitQuiz(missionId, request)

            if (response.success) {
                kotlin.Result.success(mapSubmitResponse(response.data))
            } else {
                kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            offlineDao.insertMission(
                OfflineMissionQueueEntity(
                    idempotencyKey = UUID.randomUUID().toString(),
                    missionId = missionId,
                    quizAttemptId = quizAttemptId,
                    answersJson = gson.toJson(quizAnswers),
                    attemptDate = System.currentTimeMillis()
                )
            )
            kotlin.Result.failure(
                Exception("네트워크가 불안정하여 결과가 저장되었습니다. 연결 시 자동 동기화됩니다.")
            )
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    private fun mapSubmitResponse(data: QuizSubmitResponse): QuizResult {
        val rewardsList = data.rewards.orEmpty()
        return QuizResult(
            mode = data.mode ?: "normal",
            progressApplied = data.progressApplied ?: false,
            attemptState = data.attemptState ?: "submitted",
            streakBonusApplied = data.streakBonusApplied ?: false,
            score = data.score,
            total = data.total,
            wrongCount = data.wrongCount,
            isPassed = data.isPassed,
            isPerfect = data.isPerfect,
            equippedPetGrade = data.equippedPetGrade,
            xpEarned = data.xpEarned,
            bonusXp = data.bonusXp ?: 0,
            bonusReason = data.bonusReason,
            petEvolved = data.petEvolved ?: false,
            streakDays = data.streakDays,
            todayMissionCount = data.todayMissionCount ?: 0,
            rewards = rewardsList.map {
                QuizReward(
                    type = it.type,
                    ticketType = it.ticketType,
                    count = it.count,
                    reason = it.reason
                )
            },
            remainingTickets = data.remainingTickets?.let {
                RemainingTickets(normal = it.normal, rare = it.rare, epic = it.epic)
            },
            results = data.results.orEmpty().map {
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
    }

    override suspend fun reportQuestion(
        missionId: String,
        questionId: String,
        reasonCode: String,
        detail: String?
    ): kotlin.Result<QuestionReportResult> {
        return try {
            val response = apiService.reportQuestion(
                missionId,
                questionId,
                QuestionReportRequest(reasonCode = reasonCode, detail = detail)
            )
            if (response.success) {
                val d = response.data
                kotlin.Result.success(
                    QuestionReportResult(
                        questionId = d.questionId,
                        issueId = d.issueId,
                        issueStatus = d.issueStatus,
                        quarantined = d.quarantined
                    )
                )
            } else {
                kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun syncOfflineMissions(): kotlin.Result<Unit> {
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
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
