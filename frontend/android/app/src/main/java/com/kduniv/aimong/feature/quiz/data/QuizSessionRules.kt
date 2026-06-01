package com.kduniv.aimong.feature.quiz.data

import com.kduniv.aimong.feature.quiz.data.model.QuestionResponse
import com.kduniv.aimong.feature.quiz.domain.model.TermHint
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionDifficulty
import com.kduniv.aimong.feature.quiz.domain.model.QuestionType
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import java.time.Instant
import java.time.format.DateTimeParseException

internal object QuizSessionRules {
    const val EXPECTED_QUESTION_COUNT = 10

    /** OX 피드백 패널 등 — API `correctAnswer`가 true/false일 때 O/X로 표시 */
    fun formatOxAnswerForDisplay(raw: String): String {
        when (raw.trim().lowercase()) {
            "true" -> return "O"
            "false" -> return "X"
        }
        val upper = raw.trim().uppercase()
        return if (upper == "O" || upper == "X") upper else raw.trim()
    }

    fun parseQuestionType(raw: String): kotlin.Result<QuestionType> {
        val t = raw.trim().uppercase()
        return runCatching { QuestionType.valueOf(t) }.fold(
            onSuccess = { kotlin.Result.success(it) },
            onFailure = {
                when (t) {
                    "MULTIPLE_CHOICE", "SINGLE_CHOICE" -> kotlin.Result.success(QuestionType.MULTIPLE)
                    "TRUE_FALSE" -> kotlin.Result.success(QuestionType.OX)
                    else -> kotlin.Result.failure(Exception("지원하지 않는 문제 유형입니다: $raw"))
                }
            }
        )
    }

    fun mapQuestionResponses(responses: List<QuestionResponse>): kotlin.Result<List<Question>> {
        val out = ArrayList<Question>(responses.size)
        for (r in responses) {
            val qid = r.questionId?.toString()?.takeIf { it != "0" }
                ?: r.id?.takeIf { it.isNotBlank() }
                ?: return kotlin.Result.failure(Exception("문항 ID가 없습니다."))
            val type = parseQuestionType(r.type).getOrElse { return kotlin.Result.failure(it) }
            val text = r.prompt?.takeIf { it.isNotBlank() }
                ?: r.question?.takeIf { it.isNotBlank() }
                ?: return kotlin.Result.failure(Exception("문항 내용이 비어 있습니다."))
            val opts = r.choices?.takeIf { it.isNotEmpty() } ?: r.options
            val difficulty = QuestionDifficulty.parse(r.difficulty)
            val hints = r.termHints
                .take(3)
                .mapNotNull { h ->
                    val term = h.term.trim()
                    val desc = h.description.trim()
                    if (term.isNotEmpty() && desc.isNotEmpty()) TermHint(term, desc) else null
                }
                .distinctBy { it.term }
            out.add(
                Question(
                    id = qid,
                    type = type,
                    question = text,
                    options = opts,
                    difficulty = difficulty,
                    answerFormat = r.answerFormat,
                    termHints = hints
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
        setId: String,
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
                setId = setId,
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

    /**
     * UI에서 넘긴 값(객관식·칩은 보통 1-based 인덱스 `"1"`… 또는 이미 보기 문구)을
     * check/submit body의 `answer` 문자열로 바꾼다.
     *
     * 서버가 `correctAnswer`를 **보기 텍스트**로 주므로, 선택지가 있는 유형은 항상 그 문구로 맞춘다.
     */
    /**
     * 문항 타이머 초과 시 서버 check/submit에 빈 문자열을 내면 "must not be blank"가 내려온다.
     * UI는 빈 답(시간 초과)으로 보여 주고, 저장·API용으로만 비어 있지 않은 오답 placeholder를 쓴다.
     */
    fun timeoutPlaceholderAnswer(question: Question): String {
        return when (question.type) {
            QuestionType.OX -> "O"
            else -> {
                val opts = question.options
                if (!opts.isNullOrEmpty()) {
                    normalizeAnswerForCheckPayload(question, "1")
                } else {
                    "TIMEOUT"
                }
            }
        }
    }

    /** 최종 제출용: 빈 저장값은 타임아웃 placeholder로 치환 */
    fun answerForSubmit(question: Question, stored: String): String {
        val trimmed = stored.trim()
        if (trimmed.isNotEmpty()) {
            return normalizeAnswerForCheckPayload(question, trimmed)
        }
        return timeoutPlaceholderAnswer(question)
    }

    fun normalizeAnswerForCheckPayload(question: Question, rawFromUi: String): String {
        val t = rawFromUi.trim()
        if (t.isEmpty()) return t
        if (question.type == QuestionType.OX) return t
        if (question.type != QuestionType.MULTIPLE &&
            question.type != QuestionType.FILL &&
            question.type != QuestionType.SITUATION
        ) {
            return t
        }
        val opts = question.options ?: return t
        val size = opts.size
        if (opts.any { it == t }) return t
        val index1 = t.toIntOrNull()?.takeIf { n -> size == 0 || n in 1..size } ?: return t
        return opts.getOrNull(index1 - 1)?.trim()?.takeIf { it.isNotEmpty() } ?: t
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
