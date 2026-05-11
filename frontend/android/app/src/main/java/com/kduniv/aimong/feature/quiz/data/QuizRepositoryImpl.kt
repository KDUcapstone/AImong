package com.kduniv.aimong.feature.quiz.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.dao.OfflineMissionQueueDao
import com.kduniv.aimong.core.local.dao.QuizDao
import com.kduniv.aimong.core.local.entity.OfflineMissionQueueEntity
import com.kduniv.aimong.core.local.entity.QuizMetadataEntity
import com.kduniv.aimong.core.local.entity.QuizQuestionEntity
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetAnswerItem
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizReward
import com.kduniv.aimong.feature.quiz.domain.model.RemainingTickets
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val offlineDao: OfflineMissionQueueDao,
    private val quizDao: QuizDao,
    private val gson: Gson
) : QuizRepository {

    override suspend fun getQuestionsBySetId(setId: String): kotlin.Result<QuizQuestions> {
        return try {
            val response = apiService.getMissionSetQuestions(setId)
            if (response.success) {
                mapAndPersist(response.data).also { }
            } else {
                kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
        } catch (e: HttpException) {
            fallbackCacheOr(setId) { Exception(ApiErrorMapper.userMessageForHttpException(e)) }
        } catch (e: Exception) {
            fallbackCacheOr(setId) { e }
        }
    }

    override suspend fun getQuestionsByMission(missionId: String, starLevel: Int): kotlin.Result<QuizQuestions> {
        return try {
            val response = apiService.getMissionQuestions(missionId, starLevel)
            if (response.success) {
                mapAndPersist(response.data)
            } else {
                kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    private suspend fun mapAndPersist(data: QuizQuestionsResponse): kotlin.Result<QuizQuestions> {
        val setId = data.setId?.toString()?.takeIf { it != "0" && it.isNotBlank() }
            ?: return kotlin.Result.failure(Exception("세트 정보가 없습니다."))
        val missionId = data.missionId?.toString().orEmpty()
        val title = data.missionTitle?.takeIf { it.isNotBlank() }
            ?: data.label.orEmpty().ifBlank { "학습" }
        val expiresAt = data.expiresAt?.takeIf { it.isNotBlank() } ?: "2099-12-31T23:59:59Z"
        val attempt = data.attemptId.orEmpty()
        val mapped = QuizSessionRules.mapQuestionResponses(data.questions).getOrElse {
            return kotlin.Result.failure(it)
        }
        val quiz = QuizSessionRules.buildQuizQuestions(
            setId = setId,
            missionId = missionId,
            missionTitle = title,
            isReview = data.isReview,
            quizAttemptId = attempt,
            questionCount = data.questionCount,
            expiresAt = expiresAt,
            questions = mapped
        ).getOrElse { return kotlin.Result.failure(it) }
        persistQuizCache(data, setId, missionId, title, expiresAt, attempt, data.isReview, data.questionCount)
        return kotlin.Result.success(quiz)
    }

    private suspend fun persistQuizCache(
        data: QuizQuestionsResponse,
        setId: String,
        missionId: String,
        title: String,
        expiresAt: String,
        attempt: String,
        isReview: Boolean,
        questionCount: Int
    ) {
        val metadata = QuizMetadataEntity(
            setId = setId,
            missionId = missionId,
            missionTitle = title,
            isReview = isReview,
            quizAttemptId = attempt,
            expiresAt = expiresAt,
            questionCount = questionCount
        )
        val questionEntities = data.questions.map { it ->
            val qid = it.questionId?.toString()?.takeIf { id -> id != "0" && id.isNotBlank() }
                ?: it.id.orEmpty()
            QuizQuestionEntity(
                id = qid,
                setId = setId,
                type = it.type,
                question = it.prompt?.takeIf { t -> t.isNotBlank() } ?: it.question.orEmpty(),
                optionsJson = (it.choices?.takeIf { c -> c.isNotEmpty() } ?: it.options)?.let { opt -> gson.toJson(opt) }
            )
        }
        quizDao.saveQuiz(metadata, questionEntities)
    }

    private suspend fun fallbackCacheOr(
        setId: String,
        primaryException: () -> Exception
    ): kotlin.Result<QuizQuestions> {
        val cached = loadValidCachedQuiz(setId)
        if (cached != null) return kotlin.Result.success(cached)
        return kotlin.Result.failure(primaryException())
    }

    private suspend fun loadValidCachedQuiz(setId: String): QuizQuestions? {
        val meta = quizDao.getQuizMetadata(setId) ?: return null
        val entities = quizDao.getQuizQuestions(setId)
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
            setId = meta.setId,
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
        setId: String,
        missionId: String,
        quizAttemptId: String,
        answers: Map<String, String>
    ): kotlin.Result<QuizResult> {
        val items = answers.mapNotNull { (qid, ans) ->
            val id = qid.toLongOrNull() ?: return@mapNotNull null
            MissionSetAnswerItem(questionId = id, answer = ans)
        }
        if (items.size != answers.size) {
            return kotlin.Result.failure(Exception("답안 형식이 올바르지 않습니다."))
        }
        val request = MissionSetSubmitRequest(answers = items)

        return try {
            val response = apiService.submitMissionSet(setId, request)
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
                    setId = setId,
                    missionId = missionId,
                    quizAttemptId = quizAttemptId,
                    answersJson = gson.toJson(items),
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

    override suspend fun checkAnswer(
        setId: String,
        questionId: Long,
        answer: String
    ): kotlin.Result<MissionSetCheckResponseData> {
        return try {
            val response = apiService.checkMissionSetAnswer(setId, MissionSetCheckRequest(questionId, answer))
            if (response.success) kotlin.Result.success(response.data)
            else kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun getAttempt(attemptId: String): kotlin.Result<MissionAttemptResponseData> {
        return try {
            val response = apiService.getMissionAttempt(attemptId)
            if (response.success) kotlin.Result.success(response.data)
            else kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun abandonAttempt(
        attemptId: String,
        reason: String
    ): kotlin.Result<MissionAttemptAbandonResponseData> {
        return try {
            val response = apiService.abandonMissionAttempt(attemptId, MissionAttemptAbandonRequest(reason))
            if (response.success) kotlin.Result.success(response.data)
            else kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    private fun mapSubmitResponse(data: QuizSubmitResponse): QuizResult {
        val total = data.questionCount ?: data.total ?: 10
        val correct = data.correctCount ?: 0
        val score = data.score ?: if (total > 0) correct * 100 / total else 0
        val wrong = data.wrongCount ?: (total - correct).coerceAtLeast(0)
        val rewardsList = data.rewards.orEmpty()
        return QuizResult(
            mode = data.mode ?: "normal",
            progressApplied = data.progressApplied ?: true,
            attemptState = data.attemptState ?: "submitted",
            streakBonusApplied = data.streakBonusApplied ?: false,
            score = score,
            total = total,
            wrongCount = wrong,
            isPassed = data.isPassed ?: (correct * 10 >= total * 6),
            isPerfect = data.isPerfect ?: (correct == total && wrong == 0),
            equippedPetGrade = data.equippedPetGrade,
            xpEarned = data.xpEarned ?: data.exp ?: 0,
            bonusXp = data.bonusXp ?: 0,
            bonusReason = data.bonusReason,
            petEvolved = data.petEvolved ?: false,
            streakDays = data.streakDays ?: 0,
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
                    questionId = it.questionId?.toString().orEmpty(),
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
                val answersType = object : TypeToken<List<MissionSetAnswerItem>>() {}.type
                val answers: List<MissionSetAnswerItem> = gson.fromJson(mission.answersJson, answersType)
                val request = MissionSetSubmitRequest(answers = answers)
                val response = apiService.submitMissionSet(mission.setId, request)
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
