package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData
import com.kduniv.aimong.feature.quiz.data.QuizSessionRules
import com.kduniv.aimong.feature.quiz.domain.model.AttemptStatus
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionType
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UiMode.useStubNav] 전용 — 미션/퀴즈 API 없이 10문항 세션·채점·제출이 동작한다.
 */
@Singleton
class QuizRepositoryStub @Inject constructor() : QuizRepository {

    private val sessionsByAttempt = ConcurrentHashMap<String, QuizQuestions>()
    private val sessionsBySetId = ConcurrentHashMap<String, QuizQuestions>()

    override suspend fun getQuestionsBySetId(setId: String): Result<QuizQuestions> =
        Result.success(newSession(setId = setId.ifBlank { "MOCK-SET" }, missionId = ""))

    override suspend fun getQuestionsByMission(missionId: String, starLevel: Int): Result<QuizQuestions> =
        Result.success(
            newSession(
                setId = "MOCK-${missionId.take(8)}-L$starLevel",
                missionId = missionId.ifBlank { "mock-mission" }
            )
        )

    private fun newSession(setId: String, missionId: String): QuizQuestions {
        val attemptId = "mock-attempt-${UUID.randomUUID()}"
        val questions = buildStubQuestions()
        val quiz = QuizSessionRules.buildQuizQuestions(
            setId = setId,
            missionId = missionId,
            missionTitle = "목업 미션",
            isReview = false,
            quizAttemptId = attemptId,
            questionCount = QuizSessionRules.EXPECTED_QUESTION_COUNT,
            expiresAt = "2099-12-31T23:59:59Z",
            questions = questions
        ).getOrThrow()
        sessionsByAttempt[attemptId] = quiz
        sessionsBySetId[setId] = quiz
        return quiz
    }

    private fun buildStubQuestions(): List<Question> {
        val out = ArrayList<Question>(10)
        for (i in 1..5) {
            val id = "stub_q_$i"
            out.add(
                Question(
                    id = id,
                    type = QuestionType.OX,
                    question = "목업 OX $i: 개인정보는 AI에게 함부로 알려도 된다.",
                    options = listOf("O", "X"),
                    difficulty = null,
                    answerFormat = null
                )
            )
        }
        val opts = listOf("선지 A", "선지 B", "선지 C", "선지 D")
        for (i in 6..10) {
            val id = "stub_q_$i"
            out.add(
                Question(
                    id = id,
                    type = QuestionType.MULTIPLE,
                    question = "목업 객관식 $i: 올바른 설명을 고르세요.",
                    options = opts,
                    difficulty = null,
                    answerFormat = "MULTIPLE_CHOICE"
                )
            )
        }
        return out
    }

    private fun sessionForSet(setId: String): QuizQuestions? =
        sessionsBySetId[setId] ?: sessionsByAttempt.values.firstOrNull { it.setId == setId }

    private fun correctFor(questionId: String): String {
        val idx = questionId.removePrefix("stub_q_").toIntOrNull() ?: return ""
        return if (idx in 1..5) "X" else "선지 A"
    }

    override suspend fun submitQuiz(
        setId: String,
        missionId: String,
        quizAttemptId: String,
        answers: Map<String, String>
    ): Result<QuizResult> {
        val session = sessionsByAttempt[quizAttemptId] ?: sessionForSet(setId)
            ?: return Result.failure(Exception("목업 세션을 찾을 수 없습니다."))
        val results = session.questions.map { q ->
            val submitted = answers[q.id]?.trim().orEmpty()
            val norm = QuizSessionRules.normalizeAnswerForCheckPayload(q, submitted)
            val ok = norm.equals(correctFor(q.id), ignoreCase = true)
            QuestionResult(
                questionId = q.id,
                isCorrect = ok,
                explanation = if (ok) "목업: 정답입니다." else "목업: 정답은 ${correctFor(q.id)} 입니다."
            )
        }
        val correct = results.count { it.isCorrect }
        val total = session.questionCount
        val wrong = total - correct
        return Result.success(
            QuizResult(
                mode = "normal",
                progressApplied = true,
                attemptState = AttemptStatus.SUBMITTED.name,
                streakBonusApplied = false,
                score = if (total > 0) correct * 100 / total else 0,
                total = total,
                wrongCount = wrong,
                isPassed = correct * 10 >= total * 6,
                isPerfect = correct == total,
                equippedPetGrade = null,
                xpEarned = correct * 5,
                bonusXp = 0,
                bonusReason = null,
                petEvolved = false,
                streakDays = 1,
                todayMissionCount = 1,
                rewards = emptyList(),
                remainingTickets = null,
                results = results,
                currentLevel = 2,
                currentXp = 40,
                nextLevelXp = 100
            )
        )
    }

    override suspend fun syncOfflineMissions(): Result<Unit> = Result.success(Unit)

    override suspend fun checkAnswer(
        setId: String,
        questionId: String,
        answer: String
    ): Result<MissionSetCheckResponseData> {
        val session = sessionForSet(setId) ?: return Result.failure(Exception("목업 세션이 없습니다."))
        val q = session.questions.firstOrNull { it.id == questionId }
            ?: return Result.failure(Exception("문항을 찾을 수 없습니다."))
        val norm = QuizSessionRules.normalizeAnswerForCheckPayload(q, answer.trim())
        val expected = correctFor(questionId)
        val ok = norm.equals(expected, ignoreCase = true)
        return Result.success(
            MissionSetCheckResponseData(
                questionId = questionId,
                isCorrect = ok,
                correctAnswer = expected,
                explanation = if (ok) "좋아요!" else "다시 생각해봐요."
            )
        )
    }

    override suspend fun getAttempt(attemptId: String): Result<MissionAttemptResponseData> {
        val s = sessionsByAttempt[attemptId]
            ?: return Result.failure(Exception("목업 attempt 없음"))
        return Result.success(
            MissionAttemptResponseData(
                attemptId = attemptId,
                setId = s.setId,
                missionId = s.missionId.ifBlank { null },
                starLevel = 1,
                status = "IN_PROGRESS",
                isReview = s.isReview,
                expiresAt = s.expiresAt,
                remainingSeconds = 3600,
                answeredQuestionIds = emptyList(),
                questionCount = s.questionCount
            )
        )
    }

    override suspend fun abandonAttempt(
        attemptId: String,
        reason: String
    ): Result<MissionAttemptAbandonResponseData> =
        Result.success(
            MissionAttemptAbandonResponseData(
                abandoned = true,
                attemptId = attemptId,
                status = "ABANDONED",
                energyRefunded = true
            )
        )

    override suspend fun getMissionSetReport(setId: String): Result<QuizResult> {
        val s = sessionForSet(setId) ?: return Result.failure(Exception("목업 리포트 없음"))
        val total = s.questionCount
        return Result.success(
            QuizResult(
                mode = "normal",
                progressApplied = true,
                attemptState = AttemptStatus.SUBMITTED.name,
                streakBonusApplied = false,
                score = 80,
                total = total,
                wrongCount = 2,
                isPassed = true,
                isPerfect = false,
                equippedPetGrade = null,
                xpEarned = 25,
                bonusXp = 0,
                bonusReason = null,
                petEvolved = false,
                streakDays = 1,
                todayMissionCount = 1,
                rewards = emptyList(),
                remainingTickets = null,
                results = s.questions.mapIndexed { i, q ->
                    QuestionResult(
                        questionId = q.id,
                        isCorrect = i % 3 != 0,
                        explanation = "목업 결과"
                    )
                },
                currentLevel = 2,
                currentXp = 50,
                nextLevelXp = 100
            )
        )
    }

    override suspend fun reportQuestion(
        missionId: String,
        questionId: String,
        reasonCode: String,
        detail: String?
    ): Result<QuestionReportResult> =
        Result.success(
            QuestionReportResult(
                questionId = questionId,
                issueId = "mock-issue",
                issueStatus = "OPEN",
                quarantined = false
            )
        )
}
