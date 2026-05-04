package com.kduniv.aimong.feature.quiz.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
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

            _uiState.value = QuizUiState.Loading
            quizRepository.submitQuiz(missionId, qs.quizAttemptId, userAnswers.toMap())
                .onSuccess { result ->
                    quizResult = result
                    val currentResult = result.results.find { it.questionId == questionId }
                    if (currentResult != null) {
                        _uiState.value = QuizUiState.AnswerChecked(
                            isCorrect = currentResult.isCorrect,
                            explanation = currentResult.explanation,
                            userAnswer = answer
                        )
                    } else {
                        nextQuestion()
                    }
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
     * strict 재도전: 현재까지의 답안으로 제출해 방금 문항의 정오를 확인한다.
     * @return true면 이후 [nextQuestion]을 호출하지 말 것(이미 종료/에러 상태로 전이).
     */
    suspend fun submitCurrentStepForStrictLife(): Boolean {
        if (!strictSingleLifeRetry.value || _isSolutionMode.value) return false
        val qs = cachedQuestions ?: return false
        val questions = qs.questions
        val idx = currentQuestionIndex.value
        val q = questions.getOrNull(idx) ?: return false
        if (!userAnswers.containsKey(q.id)) return false

        val res = quizRepository.submitQuiz(missionId, qs.quizAttemptId, userAnswers.toMap())
        return if (res.isSuccess) {
            val result = res.getOrNull()!!
            quizResult = result
            val step = result.results.find { it.questionId == q.id }
            when {
                step == null -> false
                !step.isCorrect -> {
                    _uiState.value = QuizUiState.Finished(result)
                    true
                }
                idx >= questions.lastIndex -> {
                    _uiState.value = QuizUiState.Finished(result)
                    true
                }
                else -> false
            }
        } else {
            _uiState.value = QuizUiState.Error(res.exceptionOrNull()?.message ?: "채점 실패")
            true
        }
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
