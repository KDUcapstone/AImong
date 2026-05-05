package com.kduniv.aimong.feature.quiz.domain.repository

import com.kduniv.aimong.feature.quiz.domain.model.QuestionCheckResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult

interface QuizRepository {
    suspend fun getQuestions(missionId: String): Result<QuizQuestions>

    /** 단일 문항 정오·해설 (v1.9 check) — 제출/보상/진행도 없음 */
    suspend fun checkQuestionAnswer(
        missionId: String,
        questionId: String,
        quizAttemptId: String,
        selected: String
    ): Result<QuestionCheckResult>

    suspend fun submitQuiz(missionId: String, quizAttemptId: String, answers: Map<String, String>): Result<QuizResult>
    suspend fun syncOfflineMissions(): Result<Unit>

    suspend fun reportQuestion(
        missionId: String,
        questionId: String,
        reasonCode: String,
        detail: String?
    ): Result<QuestionReportResult>
}
