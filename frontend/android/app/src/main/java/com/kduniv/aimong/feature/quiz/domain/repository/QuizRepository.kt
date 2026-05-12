package com.kduniv.aimong.feature.quiz.domain.repository

import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptAbandonResponseData
import com.kduniv.aimong.feature.quiz.data.model.MissionSetCheckResponseData

interface QuizRepository {
    /** 홈 등: 이미 알고 있는 setId */
    suspend fun getQuestionsBySetId(setId: String): Result<QuizQuestions>

    /** 학습맵: missionId + starLevel(1~3) */
    suspend fun getQuestionsByMission(missionId: String, starLevel: Int): Result<QuizQuestions>

    suspend fun submitQuiz(setId: String, missionId: String, quizAttemptId: String, answers: Map<String, String>): Result<QuizResult>
    suspend fun syncOfflineMissions(): Result<Unit>

    /** v2.4: 문항 단위 채점 */
    suspend fun checkAnswer(setId: String, questionId: String, answer: String): Result<MissionSetCheckResponseData>

    /** v2.4: 진행 중 attempt 복구 */
    suspend fun getAttempt(attemptId: String): Result<MissionAttemptResponseData>

    /** v2.4: 중도 이탈 */
    suspend fun abandonAttempt(attemptId: String, reason: String): Result<MissionAttemptAbandonResponseData>

    /** v2.5: 세트 제출 결과 리포트(결과 화면 보강) */
    suspend fun getMissionSetReport(setId: String): Result<QuizResult>

    suspend fun reportQuestion(
        missionId: String,
        questionId: String,
        reasonCode: String,
        detail: String?
    ): Result<QuestionReportResult>
}
