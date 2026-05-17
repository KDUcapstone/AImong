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
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptReviveRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptReviveResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetAnswerItem
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckRequest
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetReportResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetSubmitRequest
import com.kduniv.aimong.feature.quiz.data.model.QuestionReportRequest
import com.kduniv.aimong.feature.quiz.data.model.QuizQuestionsResponse
import com.kduniv.aimong.feature.quiz.data.model.QuizSubmitResponse
import com.kduniv.aimong.feature.quiz.data.model.RewardResponse
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizReward
import com.kduniv.aimong.feature.quiz.domain.model.RemainingTickets
import com.kduniv.aimong.feature.quiz.domain.model.AttemptStatus
import com.kduniv.aimong.feature.quiz.domain.model.normalizeAttemptStatus
import com.kduniv.aimong.feature.quiz.domain.model.normalizeRewardType
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
            apiService.getMissionSetQuestions(setId).toResult().fold(
                onSuccess = { mapAndPersist(it) },
                onFailure = { kotlin.Result.failure(it) }
            )
        } catch (e: HttpException) {
            fallbackCacheOr(setId) { Exception(ApiErrorMapper.userMessageForHttpException(e)) }
        } catch (e: Exception) {
            fallbackCacheOr(setId) { e }
        }
    }

    override suspend fun getQuestionsByMission(missionId: String, starLevel: Int): kotlin.Result<QuizQuestions> {
        return try {
            apiService.getMissionQuestions(missionId, starLevel).toResult().fold(
                onSuccess = { mapAndPersist(it) },
                onFailure = { kotlin.Result.failure(it) }
            )
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    private suspend fun mapAndPersist(data: QuizQuestionsResponse): kotlin.Result<QuizQuestions> {
        val setId = data.setId?.trim()?.takeIf { it != "0" && it.isNotBlank() }
            ?: return kotlin.Result.failure(Exception("세트 정보가 없습니다."))
        val missionId = data.missionId?.trim().orEmpty()
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
                    options = opts,
                    difficulty = null
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
            MissionSetAnswerItem(questionId = qid, answer = ans)
        }
        if (items.size != answers.size) {
            return kotlin.Result.failure(Exception("답안 형식이 올바르지 않습니다."))
        }
        val request = MissionSetSubmitRequest(answers = items)

        return try {
            apiService.submitMissionSet(setId, request).toResult().fold(
                onSuccess = { kotlin.Result.success(mapSubmitResponse(it)) },
                onFailure = { kotlin.Result.failure(it) }
            )
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
        questionId: String,
        answer: String
    ): kotlin.Result<MissionSetCheckResponseData> {
        return try {
            kotlin.Result.success(
                apiService.checkMissionSetAnswer(setId, MissionSetCheckRequest(questionId, answer))
                    .toResult()
                    .getOrThrow()
            )
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun getAttempt(attemptId: String): kotlin.Result<MissionAttemptResponseData> {
        return try {
            kotlin.Result.success(apiService.getMissionAttempt(attemptId).toResult().getOrThrow())
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun abandonAttempt(
        attemptId: String,
        reason: String
    ): kotlin.Result<MissionAttemptAbandonResponseData> {
        return try {
            kotlin.Result.success(
                apiService.abandonMissionAttempt(attemptId, MissionAttemptAbandonRequest(reason))
                    .toResult()
                    .getOrThrow()
            )
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun reviveAttempt(attemptId: String): kotlin.Result<MissionAttemptReviveResponseData> {
        return try {
            kotlin.Result.success(
                apiService.reviveMissionAttempt(attemptId, MissionAttemptReviveRequest(useCurrency = true))
                    .toResult()
                    .getOrThrow()
            )
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    private fun mapSubmitResponse(data: QuizSubmitResponse): QuizResult {
        val total = data.questionCount ?: data.total ?: 10
        val correct = data.correctCount ?: 0
        val score = data.score ?: if (total > 0) correct * 100 / total else 0
        val wrong = data.wrongCount ?: (total - correct).coerceAtLeast(0)
        val xpFromRewardList = data.rewards.orEmpty()
            .filter { normalizeRewardType(it.type) == "EXP" }
            .sumOf { it.count }
        val xpEarnedResolved = (data.xpEarned ?: data.exp)?.takeIf { it > 0 }
            ?: xpFromRewardList
        return QuizResult(
            mode = data.mode ?: "normal",
            progressApplied = data.progressApplied ?: true,
            attemptState = normalizeAttemptStatus(data.attemptState),
            streakBonusApplied = data.streakBonusApplied ?: false,
            score = score,
            total = total,
            wrongCount = wrong,
            isPassed = data.isPassed ?: (correct * 10 >= total * 6),
            isPerfect = data.isPerfect ?: (correct == total && wrong == 0),
            isFirstClear = data.isFirstClear == true,
            equippedPetGrade = data.equippedPetGrade,
            xpEarned = xpEarnedResolved,
            bonusXp = data.bonusXp ?: 0,
            bonusReason = data.bonusReason,
            petEvolved = data.petEvolved ?: false,
            streakDays = data.streakDays ?: 0,
            todaySetCount = data.todaySetCount ?: 0,
            rewards = mapRewardsToDomain(data.rewards),
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

    private fun mapRewardsToDomain(rewards: List<RewardResponse>?): List<QuizReward> =
        rewards.orEmpty().map {
            QuizReward(
                type = normalizeRewardType(it.type),
                ticketType = it.ticketType,
                count = it.count,
                reason = it.reason
            )
        }

    private fun mapReportResponseToQuizResult(data: MissionSetReportResponseData): QuizResult {
        val total = data.questionCount
            ?: (data.results?.takeIf { it.isNotEmpty() }?.size)
            ?: 10
        val correct = data.correctCount
            ?: (data.results?.count { it.isCorrect } ?: 0)
        val wrong = data.wrongCount ?: (total - correct).coerceAtLeast(0)
        val score = data.score ?: if (total > 0) correct * 100 / total else 0
        val xpFromRewards = data.rewards.orEmpty()
            .filter { normalizeRewardType(it.type) == "EXP" }
            .sumOf { it.count }
        return QuizResult(
            mode = if (data.isReview) "review" else "normal",
            progressApplied = true,
            attemptState = AttemptStatus.SUBMITTED.name,
            streakBonusApplied = false,
            score = score,
            total = total,
            wrongCount = wrong,
            isPassed = data.isPassed ?: (correct * 10 >= total * 6),
            isPerfect = data.isPerfect ?: (correct == total && wrong == 0),
            isFirstClear = data.isFirstClear == true,
            equippedPetGrade = null,
            xpEarned = xpFromRewards,
            bonusXp = 0,
            bonusReason = null,
            petEvolved = false,
            streakDays = 0,
            todaySetCount = 0,
            rewards = mapRewardsToDomain(data.rewards),
            remainingTickets = null,
            results = data.results.orEmpty().map { r ->
                QuestionResult(
                    questionId = r.questionId?.toString().orEmpty(),
                    isCorrect = r.isCorrect,
                    explanation = r.explanation.orEmpty(),
                    questionNo = r.questionNo,
                    correctAnswer = r.correctAnswer,
                    submittedAnswer = r.submittedAnswer
                )
            },
            currentLevel = 1,
            currentXp = 0,
            nextLevelXp = 100
        )
    }

    override suspend fun getMissionSetReport(setId: String): kotlin.Result<QuizResult> {
        return try {
            val data = apiService.getMissionSetReport(setId).toResult().getOrThrow()
            kotlin.Result.success(mapReportResponseToQuizResult(data))
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    override suspend fun reportQuestion(
        missionId: String,
        questionId: String,
        reasonCode: String,
        detail: String?
    ): kotlin.Result<QuestionReportResult> {
        return try {
            val d = apiService.reportQuestion(
                missionId,
                questionId,
                QuestionReportRequest(reasonCode = reasonCode, detail = detail)
            ).toResult().getOrThrow()
            kotlin.Result.success(
                QuestionReportResult(
                    questionId = d.questionId,
                    issueId = d.issueId,
                    issueStatus = d.issueStatus,
                    quarantined = d.quarantined
                )
            )
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
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
                if (apiService.submitMissionSet(mission.setId, request).toResult().isSuccess) {
                    offlineDao.markAsSynced(mission.id)
                }
            }
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
