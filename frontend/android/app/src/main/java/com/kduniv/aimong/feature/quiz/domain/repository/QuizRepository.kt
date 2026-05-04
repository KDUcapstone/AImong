package com.kduniv.aimong.feature.quiz.domain.repository

import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult

interface QuizRepository {
    suspend fun getQuestions(missionId: String): Result<QuizQuestions>
    suspend fun submitQuiz(missionId: String, quizAttemptId: String, answers: Map<String, String>): Result<QuizResult>
    suspend fun syncOfflineMissions(): Result<Unit>
}
