package com.kduniv.aimong.feature.quiz.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionCheckResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.data.QuizSessionRules
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val missionId: String = checkNotNull(savedStateHandle["missionId"])

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState

    private val _timeLeft = MutableStateFlow<Long>(0)
    val timeLeft: StateFlow<Long> = _timeLeft

    private var timerJob: Job? = null

    // SavedStateHandle을 통한 상태 유지
    val currentQuestionIndex: StateFlow<Int> = savedStateHandle.getStateFlow("currentIndex", 0)

    private var cachedQuestions: QuizQuestions? = null
    private val userAnswers = savedStateHandle.get<MutableMap<String, String>>("userAnswers") ?: mutableMapOf<String, String>()
    
    private val _isReviewMode = MutableStateFlow(false)
    val isReviewMode: StateFlow<Boolean> = _isReviewMode

    private val _isSolutionMode = MutableStateFlow(false)
    val isSolutionMode: StateFlow<Boolean> = _isSolutionMode

    /** 결과 화면에서 재도전 시 1하트·문항마다 제출로 정오 판정 */
    val strictSingleLifeRetry: StateFlow<Boolean> =
        savedStateHandle.getStateFlow("strictSingleLifeRetry", false)

    private var quizResult: QuizResult? = null

    init {
        fetchQuestions()
    }

    private fun fetchQuestions() {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            quizRepository.getQuestions(missionId)
                .onSuccess { questions ->
                    cachedQuestions = questions
                    _isReviewMode.value = questions.isReview
                    _uiState.value = QuizUiState.QuestionLoaded(questions)
                    startTimer(questions.expiresAt)
                }
                .onFailure {
                    _uiState.value = QuizUiState.Error(it.message ?: "Failed to load questions")
                }
        }
    }

    private fun startTimer(expiresAt: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val expiryTime = sdf.parse(expiresAt)?.time ?: return@launch
            
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val remaining = expiryTime - currentTime
                
                if (remaining <= 0) {
                    _timeLeft.value = 0
                    _uiState.value = QuizUiState.Error("세션이 만료되었습니다.")
                    break
                }
                
                _timeLeft.value = remaining
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun selectAnswer(questionId: String, answer: String) {
        if (_isSolutionMode.value) return
        userAnswers[questionId] = answer
        savedStateHandle["userAnswers"] = userAnswers // 상태 저장
    }

    /**
     * 한 문항씩 채점하고 해설을 보여주기 위한 함수
     */
    fun checkAnswer(questionId: String, answer: String) {
        viewModelScope.launch {
            val qs = cachedQuestions ?: return@launch
            userAnswers[questionId] = answer
            savedStateHandle["userAnswers"] = userAnswers

            if (UiMode.useStubNav) {
                // 목업 모드: 서버 요청 없이 로컬에서 즉시 피드백 생성
                delay(300) // 실제 느낌을 위해 약간의 지연
                val isAnswerCorrect = answer.isNotEmpty() // 빈 문자열(시간 초과)은 오답 처리
                _uiState.value = QuizUiState.AnswerChecked(
                    isCorrect = isAnswerCorrect,
                    explanation = if (isAnswerCorrect) "목업 모드 해설: 정답입니다!" else "목업 모드 해설: 시간 초과 또는 오답입니다.",
                    userAnswer = answer
                )
                // 결과 객체가 필요하므로 가상의 결과 생성
                if (quizResult == null) {
                    quizResult = QuizResult(
                        score = qs.questions.size - 1, // 가상의 점수
                        total = qs.questions.size,
                        wrongCount = 1,
                        isPassed = true,
                        isPerfect = false,
                        xpEarned = 50,
                        petEvolved = false,
                        streakDays = 7,
                        results = qs.questions.map { 
                            QuestionResult(it.id, true, "목업 해설")
                        }.toMutableList().apply {
                            this[0] = QuestionResult(qs.questions[0].id, isAnswerCorrect, "목업 해설")
                        },
                        mode = if (_isReviewMode.value) "review" else "normal",
                        equippedPetGrade = "LEGENDARY",
                        bonusXp = 10,
                        currentXp = 850,
                        nextLevelXp = 1000,
                        currentLevel = 5,
                        remainingTickets = null
                    )
                }
                return@launch
            }

            _uiState.value = QuizUiState.Loading
            quizRepository.checkQuestionAnswer(
                missionId = missionId,
                questionId = questionId,
                quizAttemptId = qs.quizAttemptId,
                selected = answer
            )
                .onSuccess { check ->
                    _uiState.value = QuizUiState.AnswerChecked(
                        isCorrect = check.isCorrect,
                        explanation = check.explanation,
                        userAnswer = answer
                    )
                }
                .onFailure {
                    _uiState.value = QuizUiState.Error(it.message ?: "채점 실패")
                }
        }
    }

    fun nextQuestion() {
        val questions = cachedQuestions?.questions ?: return
        val currentIndex = currentQuestionIndex.value

        // 만약 strict 모드에서 오답이 있었다면 결과 화면으로 종료
        if (strictSingleLifeRetry.value && quizResult != null) {
            val q = questions.getOrNull(currentIndex)
            val step = quizResult?.results?.find { it.questionId == q?.id }
            if (step != null && !step.isCorrect) {
                _uiState.value = QuizUiState.Finished(quizResult!!)
                return
            }
        }

        if (currentIndex < questions.size - 1) {
            savedStateHandle["currentIndex"] = currentIndex + 1 // 인덱스 저장
            if (_isSolutionMode.value) {
                showCurrentSolution()
            } else {
                _uiState.value = QuizUiState.QuestionLoaded(cachedQuestions!!)
            }
        } else {
            if (_isSolutionMode.value) {
                // 풀이 보기 종료 시 다시 결과 화면으로
                quizResult?.let { _uiState.value = QuizUiState.Finished(it) }
                _isSolutionMode.value = false
            } else {
                // 모든 문제를 다 푼 경우 결과 화면으로
                quizResult?.let {
                    _uiState.value = QuizUiState.Finished(it)
                } ?: run {
                    submitQuiz(cachedQuestions!!.quizAttemptId)
                }
            }
        }
    }

    fun finishQuizEarly() {
        quizResult?.let {
            _uiState.value = QuizUiState.Finished(it)
        } ?: run {
            val qs = cachedQuestions ?: return
            submitQuiz(qs.quizAttemptId)
        }
    }

    private fun submitQuiz(quizAttemptId: String) {
        viewModelScope.launch {
            val qs = cachedQuestions
            if (qs == null) {
                _uiState.value = QuizUiState.Error("문제 정보가 없습니다.")
                return@launch
            }
            if (!isAnswerSetCompleteForFullSubmit(qs)) {
                _uiState.value = QuizUiState.Error("10개 문항에 모두 답한 뒤 제출할 수 있습니다.")
                return@launch
            }
            _uiState.value = QuizUiState.Loading
            quizRepository.submitQuiz(missionId, quizAttemptId, userAnswers.toMap())
                .onSuccess { result ->
                    quizResult = result
                    _uiState.value = QuizUiState.Finished(result)
                }
                .onFailure {
                    _uiState.value = QuizUiState.Error(it.message ?: "Failed to submit quiz")
                }
        }
    }

    /** 최종 제출: 문항 수·questionId 집합이 세션과 일치해야 함 */
    private fun isAnswerSetCompleteForFullSubmit(qs: QuizQuestions): Boolean {
        if (userAnswers.size != QuizSessionRules.EXPECTED_QUESTION_COUNT) return false
        if (qs.questions.size != QuizSessionRules.EXPECTED_QUESTION_COUNT) return false
        val expected = qs.questions.map { it.id }.toSet()
        return userAnswers.keys == expected
    }

    fun startSolutionMode() {
        _isSolutionMode.value = true
        savedStateHandle["currentIndex"] = 0
        showCurrentSolution()
    }

    private fun showCurrentSolution() {
        val questions = cachedQuestions?.questions ?: return
        val result = quizResult?.results?.getOrNull(currentQuestionIndex.value) ?: return
        
        _uiState.value = QuizUiState.SolutionLoaded(
            questions[currentQuestionIndex.value],
            result.isCorrect,
            result.explanation,
            userAnswers[questions[currentQuestionIndex.value].id] ?: ""
        )
    }

    fun retryQuiz() {
        savedStateHandle["strictSingleLifeRetry"] = true
        userAnswers.clear()
        savedStateHandle["userAnswers"] = userAnswers
        savedStateHandle["currentIndex"] = 0
        _isSolutionMode.value = false
        fetchQuestions()
    }

    /**
     * strict 재도전: 현재 문항만 v1.9 check로 정오 확인. 마지막 문항까지 정답이면 그때 [submitQuiz].
     * @return true면 이후 [nextQuestion]을 호출하지 말 것(이미 종료/에러 상태로 전이).
     */
    suspend fun submitCurrentStepForStrictLife(): Boolean {
        if (!strictSingleLifeRetry.value || _isSolutionMode.value) return false
        val qs = cachedQuestions ?: return false
        val questions = qs.questions
        val idx = currentQuestionIndex.value
        val q = questions.getOrNull(idx) ?: return false
        val answer = userAnswers[q.id] ?: return false
        if (UiMode.useStubNav) return false

        val res = quizRepository.checkQuestionAnswer(missionId, q.id, qs.quizAttemptId, answer)
        return when {
            res.isFailure -> {
                _uiState.value = QuizUiState.Error(res.exceptionOrNull()?.message ?: "채점 실패")
                true
            }
            else -> {
                val check = res.getOrNull()!!
                when {
                    !check.isCorrect -> {
                        quizResult = quizResultStrictFailedAfterCheck(qs, idx, check)
                        _uiState.value = QuizUiState.Finished(quizResult!!)
                        true
                    }
                    idx >= questions.lastIndex -> {
                        val submitRes =
                            quizRepository.submitQuiz(missionId, qs.quizAttemptId, userAnswers.toMap())
                        if (submitRes.isSuccess) {
                            quizResult = submitRes.getOrNull()!!
                            _uiState.value = QuizUiState.Finished(quizResult!!)
                        } else {
                            _uiState.value = QuizUiState.Error(
                                submitRes.exceptionOrNull()?.message ?: "제출 실패"
                            )
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun quizResultStrictFailedAfterCheck(
        qs: QuizQuestions,
        failedIndex: Int,
        check: QuestionCheckResult,
    ): QuizResult {
        val results = qs.questions.mapIndexed { index, pq ->
            when {
                index < failedIndex -> QuestionResult(pq.id, true, "")
                index == failedIndex -> QuestionResult(pq.id, check.isCorrect, check.explanation)
                else -> QuestionResult(pq.id, false, "")
            }
        }
        val score = results.count { it.isCorrect }
        val wrongCount = results.count { !it.isCorrect }
        return QuizResult(
            mode = if (_isReviewMode.value) "review" else "normal",
            progressApplied = false,
            attemptState = "in_progress",
            score = score,
            total = results.size,
            wrongCount = wrongCount,
            isPassed = false,
            isPerfect = false,
            xpEarned = 0,
            petEvolved = false,
            streakDays = 0,
            results = results,
        )
    }

    fun syncOffline() {
        viewModelScope.launch {
            quizRepository.syncOfflineMissions()
        }
    }

    /** 문항 품질 신고 (reasonCode: SAFETY, INAPPROPRIATE, DUPLICATE, WRONG_ANSWER, LOW_QUALITY, ETC) */
    suspend fun reportQuestion(
        questionId: String,
        reasonCode: String,
        detail: String?
    ): Result<QuestionReportResult> {
        val trimmed = detail?.trim()?.takeIf { it.isNotEmpty() }
        return quizRepository.reportQuestion(missionId, questionId, reasonCode, trimmed)
    }
}

sealed class QuizUiState {
    object Loading : QuizUiState()
    data class QuestionLoaded(val quizQuestions: QuizQuestions) : QuizUiState()
    data class AnswerChecked(
        val isCorrect: Boolean,
        val explanation: String,
        val userAnswer: String
    ) : QuizUiState()
    data class SolutionLoaded(
        val question: Question,
        val isCorrect: Boolean,
        val explanation: String,
        val userAnswer: String
    ) : QuizUiState()
    data class Finished(val result: QuizResult) : QuizUiState()
    data class Error(val message: String) : QuizUiState()
}
