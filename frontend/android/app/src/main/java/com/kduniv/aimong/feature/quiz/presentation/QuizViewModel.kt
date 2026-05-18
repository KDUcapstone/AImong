package com.kduniv.aimong.feature.quiz.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionReportResult
import com.kduniv.aimong.feature.quiz.domain.model.QuestionResult
import com.kduniv.aimong.feature.quiz.data.QuizSessionRules
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.repository.QuizRepository
import com.kduniv.aimong.feature.home.presentation.WalletBalanceDefaults
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.quiz.data.model.MissionAttemptReviveResponseData
import retrofit2.HttpException
import com.kduniv.aimong.feature.quiz.domain.model.AttemptStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val walletRepository: WalletRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val apiService: AimongApiService
) : ViewModel() {

    private val entrySetId: String = savedStateHandle.get<String>("entrySetId").orEmpty()
    private val missionIdArg: String = savedStateHandle.get<String>("missionId").orEmpty()
    private val starLevel: Int = savedStateHandle.get<Int>("starLevel") ?: -1

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState

    private val _timeLeft = MutableStateFlow<Long>(0)
    val timeLeft: StateFlow<Long> = _timeLeft

    private var timerJob: Job? = null

    // SavedStateHandle을 통한 상태 유지
    val currentQuestionIndex: StateFlow<Int> = savedStateHandle.getStateFlow("currentIndex", 0)

    private var cachedQuestions: QuizQuestions? = null
    // SavedStateHandle에는 Serializable/Parcelable만 안정적으로 저장 가능.
    // MutableMap 그대로 저장하면 프로세스 복원 시 크래시가 날 수 있어 HashMap(Serializable)로 고정한다.
    private val userAnswers: HashMap<String, String> =
        savedStateHandle.get<HashMap<String, String>>("userAnswers") ?: hashMapOf()
    
    private val _isReviewMode = MutableStateFlow(false)
    val isReviewMode: StateFlow<Boolean> = _isReviewMode

    private val _isSolutionMode = MutableStateFlow(false)
    val isSolutionMode: StateFlow<Boolean> = _isSolutionMode

    /** 결과 화면에서 재도전 시 1하트·문항마다 제출로 정오 판정 */
    val strictSingleLifeRetry: StateFlow<Boolean> =
        savedStateHandle.getStateFlow("strictSingleLifeRetry", false)

    private var quizResult: QuizResult? = null
    private var finalSubmitJob: Job? = null
    /** 풀이 보기에서 사용할 답안 스냅샷(제출/종료 시점) */
    private var solutionAnswerSnapshot: Map<String, String> = emptyMap()

    /** v2.4: 진행 중 attempt 복구용 */
    private var attemptId: String? = null
    private var answeredQuestionIds: Set<String> = emptySet()
    private val _sessionLives = MutableStateFlow(3)
    val sessionLives: StateFlow<Int> = _sessionLives.asStateFlow()
    private var walletReviveCost: Int = WalletBalanceDefaults.HEART_REVIVE_COST
    private var walletGearBalance: Int? = null
    private val isRecoveredAttempt: Boolean
        get() = attemptId?.isNotBlank() == true && answeredQuestionIds.isNotEmpty()

    init {
        viewModelScope.launch { preloadWallet() }
        fetchQuestions()
    }

    private suspend fun preloadWallet() {
        if (UiMode.useStubNav) return
        walletRepository.getWallet().getOrNull()?.let { wallet ->
            walletReviveCost = wallet.heartReviveCost
            walletGearBalance = wallet.gear
        }
    }

    private fun fetchQuestions() {
        viewModelScope.launch {
            preloadWallet()
            _uiState.value = QuizUiState.Loading
            val loadResult = when {
                entrySetId.isNotBlank() -> loadBySetIdWithOptionalStatus(entrySetId)
                starLevel in 1..3 && missionIdArg.isNotBlank() ->
                    loadByMissionWithStatus(missionIdArg, starLevel)
                else -> kotlin.Result.failure(Exception("학습 진입 정보가 없습니다."))
            }
            loadResult
                .onSuccess { questions ->
                    cachedQuestions = questions
                    val aid = questions.quizAttemptId.trim()
                    if (aid.isNotEmpty()) {
                        attemptId = aid
                    }
                    _isReviewMode.value = questions.isReview
                    _sessionLives.value = if (questions.isReview) 1 else 3
                    // 프로세스 재생성/복원 등으로 currentIndex가 남아 있을 수 있어, 새 문제 세트 기준으로 안전 보정
                    val last = (questions.questions.size - 1).coerceAtLeast(0)
                    val clamped = currentQuestionIndex.value.coerceIn(0, last)
                    // 복구된 attempt면, 이미 답한 문항을 자동 스킵해 '멈춘 느낌'을 방지한다.
                    val startIndex = if (isRecoveredAttempt) {
                        firstUnansweredIndexFrom(clamped, questions.questions)
                    } else {
                        clamped
                    }
                    savedStateHandle["currentIndex"] = startIndex
                    _uiState.value = QuizUiState.QuestionLoaded(questions)
                    startTimer(questions.expiresAt)
                }
                .onFailure {
                    _uiState.value = QuizUiState.Error(it.message ?: "Failed to load questions")
                }
        }
    }

    private suspend fun loadBySetIdWithOptionalStatus(setId: String): kotlin.Result<QuizQuestions> {
        if (!UiMode.useStubNav && missionIdArg.isNotBlank() && starLevel in 1..3) {
            validateMissionStatus(missionIdArg, starLevel, allowInProgress = true).getOrElse {
                return kotlin.Result.failure(it)
            }
        }
        return quizRepository.getQuestionsBySetId(setId)
    }

    /** v2.4: missions/{missionId}/status로 진행중 attempt가 있으면 복구, 없으면 새 출제 */
    private suspend fun loadByMissionWithStatus(missionId: String, starLevel: Int): kotlin.Result<QuizQuestions> {
        return try {
            if (UiMode.useStubNav) {
                attemptId = null
                answeredQuestionIds = emptySet()
                return quizRepository.getQuestionsByMission(missionId, starLevel)
            }
            val statusData = apiService.getMissionStatus(missionId).toResult().getOrThrow()
            val inProgress = statusData.inProgressAttempt
            if (inProgress != null) {
                attemptId = inProgress.attemptId
                quizRepository.getAttempt(inProgress.attemptId)
                    .onSuccess { attempt ->
                        answeredQuestionIds = attempt.answeredQuestionIds.map { it.toString() }.toSet()
                        attempt.remainingLives?.let { _sessionLives.value = it.coerceIn(0, 3) }
                    }
                quizRepository.getQuestionsBySetId(inProgress.setId)
            } else {
                validateMissionStatus(missionId, starLevel, allowInProgress = false).getOrElse {
                    return kotlin.Result.failure(it)
                }
                attemptId = null
                answeredQuestionIds = emptySet()
                quizRepository.getQuestionsByMission(missionId, starLevel)
            }
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    /** v2.4 status: 잠금·에너지·별 난이도 플레이 가능 여부 */
    private suspend fun validateMissionStatus(
        missionId: String,
        starLevel: Int,
        allowInProgress: Boolean
    ): kotlin.Result<Unit> {
        if (UiMode.useStubNav) return kotlin.Result.success(Unit)
        return try {
            val status = apiService.getMissionStatus(missionId).toResult().getOrThrow()
            if (!status.isUnlocked) {
                return kotlin.Result.failure(Exception(appContext.getString(R.string.quiz_mission_locked)))
            }
            if (!allowInProgress || status.inProgressAttempt == null) {
                val energy = status.energy
                if (energy != null && energy.current < energy.required) {
                    return kotlin.Result.failure(
                        Exception(appContext.getString(R.string.quiz_insufficient_energy))
                    )
                }
            }
            val sl = status.starLevels.firstOrNull { it.starLevel == starLevel }
            if (sl != null && !sl.isPlayable) {
                return kotlin.Result.failure(Exception(appContext.getString(R.string.quiz_star_not_playable)))
            }
            kotlin.Result.success(Unit)
        } catch (e: HttpException) {
            kotlin.Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    private fun startTimer(expiresAt: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val expiryTime = parseExpiresAtMillis(expiresAt) ?: return@launch
            
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

    /**
     * 서버 `expiresAt` 포맷이 환경별로 달라질 수 있어(밀리초/타임존/Z 여부),
     * 여러 패턴을 순서대로 시도해 파싱한다.
     */
    private fun parseExpiresAtMillis(expiresAt: String): Long? {
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in candidates) {
            try {
                val sdf = SimpleDateFormat(p, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = true
                }
                val t = sdf.parse(expiresAt)?.time
                if (t != null) return t
            } catch (_: ParseException) {
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun pauseSessionTimerForSolutionMode() {
        timerJob?.cancel()
    }

    private fun resumeSessionTimerIfPossible() {
        val expiresAt = cachedQuestions?.expiresAt ?: return
        startTimer(expiresAt)
    }

    /** 신고 바텀시트 등 오버레이 동안 세션 타이머 정지 */
    fun pauseSessionTimerForOverlay() {
        timerJob?.cancel()
    }

    /** 오버레이 종료 시(풀이 모드가 아닐 때만) 세션 타이머 재개 */
    fun resumeSessionTimerAfterOverlay() {
        if (_isSolutionMode.value) return
        resumeSessionTimerIfPossible()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    /** uiState와 무관하게, 캐시된 현재 문항을 반환 */
    fun getCachedQuestionAt(index: Int): Question? {
        return cachedQuestions?.questions?.getOrNull(index)
    }

    /** uiState가 AnswerChecked여도 안전한 현재 문항 조회 */
    fun getCurrentCachedQuestion(): Question? = getCachedQuestionAt(currentQuestionIndex.value)

    fun selectAnswer(questionId: String, answer: String) {
        if (_isSolutionMode.value) return
        userAnswers[questionId] = answer
        savedStateHandle["userAnswers"] = userAnswers // 상태 저장(Serializable)
    }

    /**
     * 한 문항씩 채점하고 해설을 보여주기 위한 함수
     */
    fun checkAnswer(questionId: String, answer: String) {
        viewModelScope.launch {
            val qs = cachedQuestions ?: return@launch
            val q = qs.questions.find { it.id == questionId } ?: run {
                _uiState.value = QuizUiState.Error("문항을 찾을 수 없습니다.")
                return@launch
            }
            val payload = QuizSessionRules.normalizeAnswerForCheckPayload(q, answer)
            userAnswers[questionId] = payload
            savedStateHandle["userAnswers"] = userAnswers

            if (UiMode.useStubNav) {
                // 목업 모드: 서버 요청 없이 로컬에서 즉시 피드백 생성
                val isLastQuestion = qs.questions.indexOf(q) >= qs.questions.lastIndex
                if (!isLastQuestion) delay(80)
                val isAnswerCorrect = payload.isNotEmpty() // 빈 문자열(시간 초과)은 오답 처리
                _uiState.value = QuizUiState.AnswerChecked(
                    isCorrect = isAnswerCorrect,
                    explanation = if (isAnswerCorrect) "목업 모드 해설: 정답입니다!" else "목업 모드 해설: 시간 초과 또는 오답입니다.",
                    userAnswer = payload,
                    correctAnswer = if (isAnswerCorrect) payload else null
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
                maybePrefetchFinalSubmit()
                return@launch
            }

            // v2.4: 단건 check API로 즉시 정오·해설 반영
            if (payload.isBlank()) {
                _uiState.value = QuizUiState.AnswerChecked(
                    isCorrect = false,
                    explanation = appContext.getString(R.string.quiz_timeout_explanation),
                    userAnswer = "",
                    correctAnswer = null,
                    deferImmediateCorrectness = false
                )
                return@launch
            }
            val setId = qs.setId
            quizRepository.checkAnswer(setId, questionId, payload)
                .onSuccess { checked ->
                    val exp = checked.explanation ?: appContext.getString(R.string.quiz_answer_saved_hint)
                    checked.remainingLives?.let { _sessionLives.value = it.coerceIn(0, 3) }
                    val canRevive = checked.canRevive == true && !_isReviewMode.value
                    _uiState.value = QuizUiState.AnswerChecked(
                        isCorrect = checked.isCorrect,
                        explanation = exp,
                        userAnswer = payload,
                        correctAnswer = checked.correctAnswer,
                        deferImmediateCorrectness = false,
                        remainingLives = checked.remainingLives,
                        canRevive = canRevive,
                        reviveCost = checked.reviveCost ?: walletReviveCost,
                        gearBalance = checked.gearBalance ?: walletGearBalance
                    )
                    maybePrefetchFinalSubmit()
                }
                .onFailure { e ->
                    _uiState.value = QuizUiState.Error(e.message ?: "채점에 실패했습니다.")
                }
        }
    }

    /** 마지막 문항 채점 직후 결과 제출을 백그라운드에서 선행해 「결과 보기」 탭 시 대기를 줄인다. */
    private fun maybePrefetchFinalSubmit() {
        val qs = cachedQuestions ?: return
        if (quizResult != null) return
        if (_isSolutionMode.value) return
        if (currentQuestionIndex.value < qs.questions.lastIndex) return
        if (!isAnswerSetCompleteForFullSubmit(qs)) return
        if (finalSubmitJob?.isActive == true) return
        finalSubmitJob = viewModelScope.launch {
            performFinalSubmit(qs.quizAttemptId, showLoading = false)
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
            val desired = currentIndex + 1
            val nextIndex = if (isRecoveredAttempt) {
                firstUnansweredIndexFrom(desired, questions)
            } else {
                desired
            }
            savedStateHandle["currentIndex"] = nextIndex // 인덱스 저장
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
                resumeSessionTimerIfPossible()
            } else {
                // 모든 문제를 다 푼 경우 결과 화면으로
                quizResult?.let {
                    _uiState.value = QuizUiState.Finished(it)
                } ?: run {
                    submitQuiz(cachedQuestions!!.quizAttemptId, showLoading = true)
                }
            }
        }
    }

    fun finishQuizEarly() {
        quizResult?.let {
            _uiState.value = QuizUiState.Finished(it)
        } ?: run {
            val qs = cachedQuestions ?: return
            submitQuiz(qs.quizAttemptId, showLoading = true)
        }
    }

    /**
     * 복습 모드(하트 1개)에서 오답 발생 시 즉시 실패 처리.
     * 서버 제출 없이 현재 인덱스 기준으로 결과를 구성해 결과 화면으로 전환한다.
     */
    fun finishReviewImmediatelyOnWrong(explanation: String) {
        val qs = cachedQuestions ?: return
        val idx = currentQuestionIndex.value.coerceIn(0, qs.questions.lastIndex)
        val results = qs.questions.mapIndexed { index, q ->
            when {
                index < idx -> com.kduniv.aimong.feature.quiz.domain.model.QuestionResult(q.id, true, "")
                index == idx -> com.kduniv.aimong.feature.quiz.domain.model.QuestionResult(q.id, false, explanation)
                else -> com.kduniv.aimong.feature.quiz.domain.model.QuestionResult(q.id, false, "")
            }
        }
        // 정답 수는 전체 문항 기준으로 보이되, 오답 수는 '실제로 푼 문항' 기준으로 집계
        val score = results.take(idx + 1).count { it.isCorrect }
        val wrongCount = (idx + 1) - score
        quizResult = QuizResult(
            mode = "review",
            progressApplied = false,
            attemptState = AttemptStatus.IN_PROGRESS.name,
            score = score,
            total = results.size,
            wrongCount = wrongCount,
            isPassed = false,
            isPerfect = false,
            xpEarned = 0,
            petEvolved = false,
            streakDays = 0,
            results = results
        )
        solutionAnswerSnapshot = userAnswers.toMap()
        _uiState.value = QuizUiState.Finished(quizResult!!)
    }

    private fun submitQuiz(quizAttemptId: String, showLoading: Boolean) {
        viewModelScope.launch {
            awaitFinalSubmitOrPerform(quizAttemptId, showLoading)
        }
    }

    private suspend fun awaitFinalSubmitOrPerform(quizAttemptId: String, showLoading: Boolean) {
        finalSubmitJob?.join()
        if (quizResult != null) {
            _uiState.value = QuizUiState.Finished(quizResult!!)
            return
        }
        performFinalSubmit(quizAttemptId, showLoading)
    }

    private suspend fun performFinalSubmit(quizAttemptId: String, showLoading: Boolean) {
        if (quizResult != null) {
            _uiState.value = QuizUiState.Finished(quizResult!!)
            return
        }
        val qs = cachedQuestions
        if (qs == null) {
            _uiState.value = QuizUiState.Error("문제 정보가 없습니다.")
            return
        }
        if (!isRecoveredAttempt && !isAnswerSetCompleteForFullSubmit(qs)) {
            _uiState.value = QuizUiState.Error("10개 문항에 모두 답한 뒤 제출할 수 있습니다.")
            return
        }
        if (showLoading) {
            _uiState.value = QuizUiState.Loading
        }
        val reportDeferred: Deferred<kotlin.Result<QuizResult>>? =
            if (!UiMode.useStubNav) {
                viewModelScope.async {
                    quizRepository.getMissionSetReport(qs.setId)
                }
            } else {
                null
            }
        quizRepository.submitQuiz(
            setId = qs.setId,
            missionId = qs.missionId.ifBlank { missionIdArg },
            quizAttemptId = quizAttemptId,
            answers = userAnswers.toMap(),
        )
            .onSuccess { result ->
                solutionAnswerSnapshot = userAnswers.toMap()
                val merged = if (result.results.isEmpty()) {
                    reportDeferred?.await()?.getOrElse { result } ?: result
                } else {
                    reportDeferred?.cancel()
                    result
                }
                quizResult = merged
                _uiState.value = QuizUiState.Finished(merged)
            }
            .onFailure {
                reportDeferred?.cancel()
                _uiState.value = QuizUiState.Error(it.message ?: "Failed to submit quiz")
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
        pauseSessionTimerForSolutionMode()
        savedStateHandle["currentIndex"] = 0
        showCurrentSolution()
    }

    private fun showCurrentSolution() {
        val questions = cachedQuestions?.questions ?: return
        val result = quizResult?.results?.getOrNull(currentQuestionIndex.value) ?: return
        val q = questions.getOrNull(currentQuestionIndex.value) ?: return
        val userAnswer = solutionAnswerSnapshot[q.id] ?: userAnswers[q.id] ?: ""

        _uiState.value = QuizUiState.SolutionLoaded(
            q,
            result.isCorrect,
            result.explanation,
            userAnswer
        )
    }

    fun retryQuiz() {
        savedStateHandle["strictSingleLifeRetry"] = true
        // 이전 텀(결과/풀이보기)의 상태가 두 번째 텀에 섞이지 않도록, ViewModel 내부 상태를 강제 초기화한다.
        timerJob?.cancel()
        _timeLeft.value = 0
        cachedQuestions = null
        quizResult = null
        solutionAnswerSnapshot = emptyMap()
        _isSolutionMode.value = false
        attemptId = null
        answeredQuestionIds = emptySet()

        userAnswers.clear()
        savedStateHandle["userAnswers"] = userAnswers
        savedStateHandle["currentIndex"] = 0
        fetchQuestions()
    }

    fun isAnsweredAlready(questionId: String): Boolean = answeredQuestionIds.contains(questionId)

    private fun firstUnansweredIndexFrom(start: Int, questions: List<Question>): Int {
        if (questions.isEmpty()) return 0
        var i = start.coerceIn(0, questions.lastIndex)
        while (i <= questions.lastIndex) {
            val qid = questions[i].id
            if (!answeredQuestionIds.contains(qid)) return i
            i++
        }
        // 모두 answered면 마지막을 유지(이후 결과/종료로 가는 UX는 Fragment에서 처리)
        return questions.lastIndex
    }

    suspend fun abandonCurrentAttemptIfAny(reason: String): Result<Unit> {
        val id = attemptId?.takeIf { it.isNotBlank() } ?: return Result.success(Unit)
        return quizRepository.abandonAttempt(id, reason).map { Unit }
    }

    /** v2.7: 톱니바퀴로 하트 부활 (일반 모드·attempt당 1회) */
    suspend fun reviveCurrentAttempt(): Result<MissionAttemptReviveResponseData> {
        val id = attemptId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(Exception("진행 중인 세션이 없습니다."))
        if (_isReviewMode.value) {
            return Result.failure(Exception(appContext.getString(R.string.quiz_revive_not_in_review)))
        }
        return quizRepository.reviveAttempt(id).onSuccess { data ->
            _sessionLives.value = data.remainingLives.coerceIn(0, 3)
            data.gearBalance?.let { walletGearBalance = it }
        }
    }

    /** 톱니 부활 후 같은 문항에서 다시 풀 수 있도록 UI 상태를 문항 로드로 되돌린다. */
    fun resumeAfterGearRevive() {
        val qs = cachedQuestions ?: return
        _uiState.value = QuizUiState.QuestionLoaded(qs)
    }

    /**
     * 단건 check API 미사용으로, 문항별 즉시 정오 판정은 불가합니다. (최종 [submitQuiz]로만 확인)
     */
    suspend fun submitCurrentStepForStrictLife(): Boolean = false

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
        val mid = cachedQuestions?.missionId?.takeIf { it.isNotBlank() } ?: missionIdArg
        return quizRepository.reportQuestion(mid, questionId, reasonCode, trimmed)
    }
}

sealed class QuizUiState {
    object Loading : QuizUiState()
    data class QuestionLoaded(val quizQuestions: QuizQuestions) : QuizUiState()
    data class AnswerChecked(
        val isCorrect: Boolean,
        val explanation: String,
        val userAnswer: String,
        val correctAnswer: String? = null,
        val deferImmediateCorrectness: Boolean = false,
        val remainingLives: Int? = null,
        val canRevive: Boolean = false,
        val reviveCost: Int? = null,
        val gearBalance: Int? = null
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
