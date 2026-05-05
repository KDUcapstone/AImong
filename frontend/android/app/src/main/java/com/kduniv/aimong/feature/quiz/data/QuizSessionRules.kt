package com.kduniv.aimong.feature.quiz.data

import com.kduniv.aimong.feature.quiz.data.model.QuestionResponse
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionType
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import java.time.Instant
import java.time.format.DateTimeParseException

internal object QuizSessionRules {
    const val EXPECTED_QUESTION_COUNT = 10

    fun parseQuestionType(raw: String): kotlin.Result<QuestionType> =
        runCatching { QuestionType.valueOf(raw.trim()) }.fold(
            onSuccess = { kotlin.Result.success(it) },
            onFailure = {
                kotlin.Result.failure(
                    Exception("지원하지 않는 문제 유형입니다: $raw")
                )
            }
        )

    fun mapQuestionResponses(responses: List<QuestionResponse>): kotlin.Result<List<Question>> {
        val out = ArrayList<Question>(responses.size)
        for (r in responses) {
            val type = parseQuestionType(r.type).getOrElse { return kotlin.Result.failure(it) }
            out.add(
                Question(
                    id = r.id,
                    type = type,
                    question = r.question,
                    options = r.options
                )
            )
        }
        return kotlin.Result.success(out)
    }

    fun validateQuestionPayload(questionCount: Int, questions: List<Question>): kotlin.Result<Unit> {
        if (questionCount != EXPECTED_QUESTION_COUNT) {
            return kotlin.Result.failure(
                Exception("문항 수가 올바르지 않습니다. (서버 questionCount=$questionCount, 기대값=$EXPECTED_QUESTION_COUNT)")
            )
        }
        if (questions.size != EXPECTED_QUESTION_COUNT) {
            return kotlin.Result.failure(
                Exception("문항 수가 올바르지 않습니다. (받은 문항 ${questions.size}개, 기대값=$EXPECTED_QUESTION_COUNT)")
            )
        }
        return kotlin.Result.success(Unit)
    }

    fun buildQuizQuestions(
        missionId: String,
        missionTitle: String,
        isReview: Boolean,
        quizAttemptId: String,
        questionCount: Int,
        expiresAt: String,
        questions: List<Question>
    ): kotlin.Result<QuizQuestions> {
        validateQuestionPayload(questionCount, questions).getOrElse {
            return kotlin.Result.failure(it)
        }
        return kotlin.Result.success(
            QuizQuestions(
                missionId = missionId,
                missionTitle = missionTitle,
                isReview = isReview,
                quizAttemptId = quizAttemptId,
                questionCount = questionCount,
                expiresAt = expiresAt,
                questions = questions
            )
        )
    }

    fun isSessionExpired(expiresAtIso: String): Boolean {
        val exp = parseExpiryMillis(expiresAtIso) ?: return true
        return System.currentTimeMillis() >= exp
    }

    private fun parseExpiryMillis(expiresAtIso: String): Long? {
        return try {
            Instant.parse(expiresAtIso).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
