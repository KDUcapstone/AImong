package com.kduniv.aimong.feature.quiz.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorRes
import android.graphics.Typeface
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentQuizBinding
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionDifficulty
import com.kduniv.aimong.feature.quiz.domain.model.QuestionType
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizReward
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import com.kduniv.aimong.feature.quiz.domain.model.TermHint
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.drawable.GradientDrawable

@AndroidEntryPoint
class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()

    private fun quizColor(@ColorRes resId: Int): Int =
        ContextCompat.getColor(requireContext(), resId)

    private var lives = 3
    private var heartsAtQuizStart = 3
    /** 세션 누적 하트 소모(부활로 잔량이 회복되어도 감소하지 않음) */
    private var totalHeartsLost = 0
    private var maxPlayedIndex = 0
    private var timer: CountDownTimer? = null
    private var questionTimeLeftMs: Long = 30000L
    private var _isAdded = false
    private var isRetryingFromResult = false

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _isAdded = true
    }

    override fun onDestroyView() {
        timer?.cancel()
        timer = null
        super.onDestroyView()
        _isAdded = false
    }

    private fun startTimer(reset: Boolean) {
        timer?.cancel()
        if (_binding == null) return
        if (reset) questionTimeLeftMs = 30000L
        timer = object : CountDownTimer(questionTimeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                questionTimeLeftMs = millisUntilFinished
                binding.tvTimer.text = "⏱ ${millisUntilFinished / 1000}초 남음"
                if (millisUntilFinished <= 10000) {
                    binding.tvTimer.setTextColor(Color.RED)
                } else {
                    binding.tvTimer.setTextColor(quizColor(R.color.quiz_text_grey))
                }
            }
            override fun onFinish() {
                if (_binding == null) return
                questionTimeLeftMs = 0
                binding.tvTimer.text = "⏱ 0초 남음"
                if (binding.layoutFeedbackPanel.visibility != View.VISIBLE) {
                    handleOptionClick("")
                }
            }
        }.start()
    }

    override fun initView() {
        binding.ivBack.setOnClickListener {
            // v2.4: 진행 중 attempt가 있으면 중도 이탈 기록 후 종료
            lifecycleScope.launch {
                viewModel.abandonCurrentAttemptIfAny("USER_EXIT")
                findNavController().popBackStack()
            }
        }
        binding.btnResViewSolutions.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            viewModel.startSolutionMode()
        }
        binding.btnResRetry.setOnClickListener {
            if (isRetryingFromResult) return@setOnClickListener
            isRetryingFromResult = true

            // 결과 화면에서 재시도 시, Fragment 로컬 상태를 먼저 정리해 크래시/꼬임을 방지한다.
            timer?.cancel()
            questionTimeLeftMs = 30000L
            lives = 3
            heartsAtQuizStart = 3
            totalHeartsLost = 0
            maxPlayedIndex = 0

            binding.layoutFeedbackPanel.visibility = View.GONE
            binding.btnFeedbackRefillHearts.visibility = View.GONE
            binding.layoutQuizResult.visibility = View.GONE
            unlockOptionsForNewQuestion()
            resetOxButtons()
            resetMultipleFixedOptions()

            binding.btnResRetry.isEnabled = false
            binding.btnResRetry.alpha = 0.5f
            viewModel.retryQuiz()
        }
        binding.btnResFinish.setOnClickListener {
            lifecycleScope.launch {
                viewModel.abandonCurrentAttemptIfAny("USER_EXIT")
                findNavController().popBackStack()
            }
        }
        binding.btnNextQuestion.setOnClickListener {
            // 결과 화면에서 "다시하기"를 누른 직후(두 번째 텀 로딩 중)엔 이전 텀의 피드백/전이와 충돌하지 않게 막는다.
            if (isRetryingFromResult) return@setOnClickListener
            binding.layoutFeedbackPanel.visibility = View.GONE
            binding.btnFeedbackRefillHearts.visibility = View.GONE
            restoreDefaultFeedbackButtonStyles()
            viewModel.nextQuestion()
        }
        binding.btnFeedbackRefillHearts.setOnClickListener {
            requestReviveWithGear()
        }
        binding.btnOxO.setOnClickListener { 
            animateSelection(it)
            applyOxPendingSelection("O")
            handleOptionClick("O") 
        }
        binding.btnOxX.setOnClickListener { 
            animateSelection(it)
            applyOxPendingSelection("X")
            handleOptionClick("X") 
        }
        parentFragmentManager.setFragmentResultListener(
            QuizReportBottomSheet.REQUEST_KEY_SUBMIT,
            viewLifecycleOwner
        ) { _, bundle ->
            val reasonCode = bundle.getString(QuizReportBottomSheet.RESULT_REASON_CODE) ?: return@setFragmentResultListener
            val detail = bundle.getString(QuizReportBottomSheet.RESULT_DETAIL)
            submitQuestionReport(reasonCode, detail)
        }
        parentFragmentManager.setFragmentResultListener(
            QuizReportBottomSheet.REQUEST_KEY_DISMISS,
            viewLifecycleOwner
        ) { _, _ ->
            // dismiss 시 타이머 즉시 재개
            viewModel.resumeSessionTimerAfterOverlay()
            if (!viewModel.isSolutionMode.value &&
                binding.layoutQuizResult.visibility != View.VISIBLE &&
                binding.layoutFeedbackPanel.visibility != View.VISIBLE
            ) {
                if (questionTimeLeftMs > 0) startTimer(reset = false)
            }
        }

        binding.btnReportQuestion.setOnClickListener { showQuestionReportReasonDialog() }
        updateReportButtonVisibility()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
                launch {
                    viewModel.currentQuestionIndex.collect { index ->
                        if (!viewModel.isSolutionMode.value) {
                            updateQuestion(index)
                        }
                        binding.pbQuizProgress.progress = index + 1
                    }
                }
                launch {
                    viewModel.isReviewMode.collect { isReview ->
                        // 결과 화면에서는 '복습' 뱃지 노출 금지
                        binding.cardReviewBadge.visibility =
                            if (binding.layoutQuizResult.visibility == View.VISIBLE) View.GONE
                            else if (isReview) View.VISIBLE
                            else View.GONE
                        if (isReview) {
                            binding.tvExpInfo.text = "복습 시 EXP 50% 획득"
                            binding.tvExpInfo.setTextColor(quizColor(R.color.quiz_hint_gold))
                        }
                        updateQuizModeBanner()
                    }
                }
                launch {
                    viewModel.sessionLives.collect { count ->
                        if (!viewModel.isSolutionMode.value && binding.layoutFeedbackPanel.visibility != View.VISIBLE) {
                            updateHearts(count.coerceIn(0, 3), forceReset = true)
                        }
                    }
                }
                launch {
                    viewModel.isSolutionMode.collect { isSolution ->
                        if (isSolution) {
                            // 풀이 모드에서는 문항 30초 타이머 정지 + 잔상 제거
                            timer?.cancel()
                            binding.tvTimer.text = "풀이 보기"
                            binding.tvTimer.setTextColor(quizColor(R.color.quiz_text_grey))
                        }
                    }
                }
            }
        }
    }

    private fun handleUiState(state: QuizUiState) {
        when (state) {
            is QuizUiState.Loading -> {
                // 로딩 표시
            }
            is QuizUiState.QuestionLoaded -> {
                if (isRetryingFromResult) {
                    isRetryingFromResult = false
                    binding.btnResRetry.isEnabled = true
                    binding.btnResRetry.alpha = 1f
                }
                // Error 상태에서 '다시 시도'로 덮어쓴 리스너/텍스트가 남아있을 수 있어 원복
                if (binding.btnNextQuestion.text?.toString() == "다시 시도") {
                    binding.btnNextQuestion.text = getString(R.string.quiz_btn_next)
                    binding.btnNextQuestion.setOnClickListener {
                        binding.layoutFeedbackPanel.visibility = View.GONE
                        binding.btnFeedbackRefillHearts.visibility = View.GONE
                        viewModel.nextQuestion()
                    }
                }
                updateQuestion(viewModel.currentQuestionIndex.value)
            }
            is QuizUiState.AnswerChecked -> {
                val q =
                    viewModel.getCurrentCachedQuestion()
                        ?: viewModel.getCachedQuestionAt(viewModel.currentQuestionIndex.value)
                if (q != null) {
                    showAnswerFeedback(
                        q,
                        state.isCorrect,
                        state.explanation,
                        state.userAnswer,
                        state.correctAnswer,
                        state.deferImmediateCorrectness,
                        state.remainingLives,
                        state.canRevive,
                        state.reviveCost,
                        state.gearBalance
                    )
                } else {
                    // 캐시가 비어있으면 기존 동작(최소한 패널은 띄우지 않고 토스트만)
                    Toast.makeText(requireContext(), "문제 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            is QuizUiState.SolutionLoaded -> {
                showSolution(state)
            }
            is QuizUiState.Finished -> {
                // 결과 화면이 올라올 때 피드백 패널/타이머 잔상 제거
                timer?.cancel()
                binding.layoutFeedbackPanel.visibility = View.GONE
                showResult(state.result)
            }
            is QuizUiState.Error -> {
                if (isRetryingFromResult) {
                    isRetryingFromResult = false
                    binding.btnResRetry.isEnabled = true
                    binding.btnResRetry.alpha = 1f
                }
                if (state.message == "세션이 만료되었습니다.") {
                    showFeedback("만료", state.message)
                } else if (state.message.contains("문제 세트를 준비하는 데 실패했습니다")) {
                    showMissionSetNotReadyDialog()
                } else {
                    // 채점 오류 등에서 옵션이 잠긴 채로 남으면 UX가 '멈춘 것처럼' 보이므로 즉시 복구한다.
                    Log.e("QuizFragment", "Quiz error state: ${state.message}")
                    showFeedback("잠시 후 다시 시도해줘", state.message)
                    binding.btnNextQuestion.text = "다시 시도"
                    binding.btnNextQuestion.setOnClickListener {
                        binding.layoutFeedbackPanel.visibility = View.GONE
                        binding.btnFeedbackRefillHearts.visibility = View.GONE
                        unlockOptionsForNewQuestion()
                        // 문항 타이머는 현 문항에서 이어서 진행
                        if (!viewModel.isSolutionMode.value && binding.layoutQuizResult.visibility != View.VISIBLE) {
                            if (questionTimeLeftMs > 0) startTimer(reset = false)
                        }
                    }
                    unlockOptionsForNewQuestion()
                }
            }
        }
        updateReportButtonVisibility()
    }

    private fun updateReportButtonVisibility() {
        binding.btnReportQuestion.visibility = when (viewModel.uiState.value) {
            is QuizUiState.Loading, is QuizUiState.Finished -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun showQuestionReportReasonDialog() {
        // 앱 테마에 맞는 바텀시트로 노출
        timer?.cancel()
        viewModel.pauseSessionTimerForOverlay()
        QuizReportBottomSheet.newInstance()
            .show(parentFragmentManager, "QuizReportBottomSheet")
    }

    private fun submitQuestionReport(reasonCode: String, detail: String?) {
        val q = currentQuestionForReport() ?: run {
            Toast.makeText(requireContext(), R.string.quiz_report_no_question, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            viewModel.reportQuestion(q.id, reasonCode, detail)
                .onSuccess {
                    Toast.makeText(requireContext(), R.string.quiz_report_success, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.message.orEmpty(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun currentQuestionForReport(): Question? {
        return when (val s = viewModel.uiState.value) {
            is QuizUiState.SolutionLoaded -> s.question
            else -> getCurrentQuestion()
        }
    }

    private fun showSolution(state: QuizUiState.SolutionLoaded) {
        val index = viewModel.currentQuestionIndex.value
        val total = (viewModel.uiState.value as? QuizUiState.QuestionLoaded)?.quizQuestions?.questions?.size ?: 10
        
        binding.tvQuestionCount.text = "${index + 1} / $total 문제"
        bindQuestionDifficultyStars(state.question.difficulty)
        binding.tvQuizQuestion.text = state.question.question
        binding.pbQuizProgress.progress = index + 1

        setupOptions(state.question)
        // 풀이 모드에서는 클릭 방지 및 정답 표시
        lockOptions()
        markCorrectAnswer(state.question, state.userAnswer, state.isCorrect)
        
        showAnswerFeedback(state.question, state.isCorrect, state.explanation, state.userAnswer, null, false)

        val targetSize = if (lives <= 0) maxPlayedIndex else total - 1
        val isLast = index >= targetSize
        binding.btnNextQuestion.text = if (isLast) "결과로 돌아가기" else "다음 풀이 →"
        
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            binding.btnFeedbackRefillHearts.visibility = View.GONE
            if (isLast) {
                viewModel.finishQuizEarly() // 결과 화면으로 전환
            } else {
                viewModel.nextQuestion()
            }
        }
    }

    private fun markCorrectAnswer(question: Question, userAnswer: String, isCorrect: Boolean) {
        val density = resources.displayMetrics.density
        val isSolutionMode = viewModel.isSolutionMode.value
        
        // FILL 유형: 질문 텍스트의 빈칸을 사용자 답변으로 교체 (v2.3 명세 준수)
        if (question.type == QuestionType.FILL) {
            val typeLabel = "단어 채우기"
            val originalText = question.question
            val displayWord = choiceIndexToDisplayText(question, userAnswer) ?: userAnswer
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $displayWord ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $displayWord ")
                else -> originalText
            }
            val fullText = "[$typeLabel] $replacedText"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            // 문제 본문 색은 정오와 무관하게 고정(옵션 영역에서만 정오 표시)
            binding.tvQuizQuestion.setTextColor(quizColor(R.color.quiz_text_primary))
        }

        // OX 유형
        if (question.type == QuestionType.OX) {
            // 이전 문항/모드의 스트로크 잔상 제거 후 정오 표시
            resetOxButtons()
            val correctLabel = if (isCorrect) userAnswer else (if (userAnswer == "O") "X" else "O")
            if (correctLabel == "O") {
                binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_mint)))
                binding.btnOxO.setStrokeWidth((8 * density).toInt())
            } else {
                binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_mint)))
                binding.btnOxX.setStrokeWidth((8 * density).toInt())
            }
            
            if (!isCorrect) {
                if (userAnswer == "O") {
                    binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_red)))
                    binding.btnOxO.setStrokeWidth((8 * density).toInt())
                } else {
                    binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_red)))
                    binding.btnOxX.setStrokeWidth((8 * density).toInt())
                }
            }
        } 
        // 객관식·단어채우기·상황판단: 동일 카드 UI
        else {
            val idx = choiceIndexFromUserAnswer(question, userAnswer) ?: return
            applyChoiceCardAnswerHighlight(idx.coerceIn(0, 3), isCorrect, isSolutionMode)
        }
    }

    private fun applyChoiceCardAnswerHighlight(selectedIndex: Int, isCorrect: Boolean, isSolutionMode: Boolean) {
        val cards = listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4,
        )
        val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        val density = resources.displayMetrics.density
        if (selectedIndex !in cards.indices) return
        val positive = isSolutionMode || isCorrect
        val bgColor = ContextCompat.getColor(
            requireContext(),
            if (positive) R.color.quiz_mint else R.color.quiz_red,
        )
        cards.forEachIndexed { idx, card ->
            if (idx == selectedIndex) {
                card.setCardBackgroundColor(bgColor)
                card.strokeWidth = (3 * density).toInt()
                card.strokeColor = bgColor
                texts[idx].setTextColor(Color.WHITE)
            }
        }
    }

    /** 객관식 고정 카드: 선택 인덱스 0..3 */
    private fun applyMultipleFixedSelectionByIndex(selectedIndex: Int) {
        val cards = listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        )
        val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        val density = resources.displayMetrics.density
        if (selectedIndex !in cards.indices) return
        cards.forEachIndexed { idx, card ->
            if (idx == selectedIndex) {
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_mint))
                card.strokeWidth = (3 * density).toInt()
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.quiz_mint)
                texts[idx].setTextColor(Color.WHITE)
            }
        }
    }

    /** 사용자 답(번호 문자열 또는 보기 문구) → options 내 0-based 인덱스 */
    private fun choiceIndexFromUserAnswer(question: Question, userAnswer: String): Int? {
        val t = userAnswer.trim()
        val opts = question.options ?: return null
        val n = t.toIntOrNull()
        if (n != null && n in 1..opts.size) return n - 1
        val i = opts.indexOfFirst { it == t }
        return i.takeIf { it >= 0 }
    }

    private fun choiceIndexToDisplayText(question: Question, answerKey: String): String? {
        val t = answerKey.trim()
        if (t.isEmpty()) return null
        t.toIntOrNull()?.minus(1)?.let { i -> question.options?.getOrNull(i)?.let { return it } }
        if (question.options?.any { it == t } == true) return t
        return t
    }

    private fun formatCorrectAnswerLine(question: Question, correctAnswer: String?): String? {
        val ca = correctAnswer?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val opts = question.options
        val num = ca.toIntOrNull()
        if (opts != null && num != null && num in 1..opts.size) {
            return "정답: ${opts[num - 1]}"
        }
        return "정답: $ca"
    }

    private fun popQuizToHomeAfterAbandon(reason: String) {
        lifecycleScope.launch {
            try {
                viewModel.abandonCurrentAttemptIfAny(reason)
            } catch (_: Exception) {
            }
            if (!isAdded) return@launch
            Toast.makeText(requireContext(), R.string.quiz_hearts_exhausted_toast, Toast.LENGTH_SHORT).show()
            val nav = findNavController()
            val popped = runCatching { nav.popBackStack(R.id.homeFragment, false) }.getOrDefault(false)
            if (!popped) {
                nav.popBackStack()
            }
        }
    }

    private fun bindLivesExhaustedFeedbackActions(canRevive: Boolean, reviveCost: Int?, gearBalance: Int?) {
        if (canRevive) {
            binding.btnFeedbackRefillHearts.visibility = View.VISIBLE
            val cost = reviveCost ?: com.kduniv.aimong.feature.home.presentation.WalletBalanceDefaults.HEART_REVIVE_COST
            val balance = gearBalance ?: 0
            binding.btnFeedbackRefillHearts.text = getString(R.string.quiz_revive_cost_fmt, cost, balance)
            styleFeedbackGearButtonPrimary()
        } else {
            binding.btnFeedbackRefillHearts.visibility = View.GONE
        }
        styleFeedbackHomeButtonSecondary()
        binding.btnNextQuestion.text = getString(R.string.quiz_btn_return_home)
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            binding.btnFeedbackRefillHearts.visibility = View.GONE
            restoreDefaultFeedbackButtonStyles()
            popQuizToHomeAfterAbandon("LIVES_EXHAUSTED")
        }
    }

    /** 하트 소진 시: 홈으로 돌아가기 — 회색 보조 버튼 */
    private fun styleFeedbackHomeButtonSecondary() {
        binding.btnNextQuestion.backgroundTintList =
            ColorStateList.valueOf(quizColor(R.color.quiz_button_secondary_bg))
        binding.btnNextQuestion.setTextColor(quizColor(R.color.quiz_text_grey))
        binding.btnNextQuestion.strokeWidth = 0
    }

    /** 하트 소진 시: 톱니바퀴 부활 — 초록 강조 버튼 */
    private fun styleFeedbackGearButtonPrimary() {
        binding.btnFeedbackRefillHearts.backgroundTintList =
            ColorStateList.valueOf(quizColor(R.color.quiz_mint))
        binding.btnFeedbackRefillHearts.setTextColor(quizColor(R.color.quiz_on_accent))
        binding.btnFeedbackRefillHearts.strokeWidth = 0
        binding.btnFeedbackRefillHearts.strokeColor =
            ColorStateList.valueOf(quizColor(R.color.quiz_mint))
    }

    private fun restoreDefaultFeedbackButtonStyles() {
        binding.btnNextQuestion.backgroundTintList =
            ColorStateList.valueOf(quizColor(R.color.quiz_mint))
        binding.btnNextQuestion.setTextColor(quizColor(R.color.quiz_on_accent))
        binding.btnNextQuestion.strokeWidth = 0
        val strokePx = (1f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        binding.btnFeedbackRefillHearts.backgroundTintList =
            ColorStateList.valueOf(Color.TRANSPARENT)
        binding.btnFeedbackRefillHearts.setTextColor(quizColor(R.color.quiz_feedback_text))
        binding.btnFeedbackRefillHearts.strokeWidth = strokePx
        binding.btnFeedbackRefillHearts.strokeColor =
            ColorStateList.valueOf(quizColor(R.color.quiz_text_grey))
    }

    private fun requestReviveWithGear() {
        lifecycleScope.launch {
            viewModel.reviveCurrentAttempt().fold(
                onSuccess = { data ->
                    resumeQuizAfterGearRevive(data.remainingLives.coerceIn(0, 3))
                    Toast.makeText(requireContext(), getString(R.string.quiz_btn_refill_hearts), Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    Toast.makeText(
                        requireContext(),
                        e.message ?: getString(R.string.quiz_hearts_exhausted_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    /** 톱니 부활 성공 후 피드백·타이머·선택지를 복구해 같은 문항을 이어서 푼다. */
    private fun resumeQuizAfterGearRevive(remainingLives: Int) {
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.btnFeedbackRefillHearts.visibility = View.GONE
        restoreDefaultFeedbackButtonStyles()
        updateHearts(remainingLives, forceReset = true)
        lives = remainingLives
        viewModel.resumeAfterGearRevive()

        val question = viewModel.getCurrentCachedQuestion() ?: getCurrentQuestion() ?: return
        resetOxButtons()
        resetMultipleFixedOptions()
        setupOptions(question)
        binding.tvAttemptRecoverBanner.visibility = View.GONE

        val total = (viewModel.uiState.value as? QuizUiState.QuestionLoaded)
            ?.quizQuestions?.questions?.size ?: binding.pbQuizProgress.max
        val index = viewModel.currentQuestionIndex.value
        val isLast = index >= total - 1
        binding.btnNextQuestion.text =
            if (isLast) getString(R.string.quiz_btn_view_result) else getString(R.string.quiz_btn_next)
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            restoreDefaultFeedbackButtonStyles()
            viewModel.nextQuestion()
        }

        if (!viewModel.isSolutionMode.value && binding.layoutQuizResult.visibility != View.VISIBLE) {
            startTimer(reset = true)
        }
    }

    private fun showAnswerFeedback(
        question: Question,
        isCorrect: Boolean,
        explanation: String,
        userAnswer: String,
        correctAnswer: String? = null,
        deferImmediateCorrectness: Boolean = false,
        serverRemainingLives: Int? = null,
        canRevive: Boolean = false,
        reviveCost: Int? = null,
        gearBalance: Int? = null
    ) {
        binding.layoutQuizResult.visibility = View.GONE
        binding.btnFeedbackRefillHearts.visibility = View.GONE
        if (!viewModel.isSolutionMode.value) {
            binding.tvQuizModeBanner.visibility = View.GONE
        }

        val livesExhausted = (serverRemainingLives ?: lives) <= 0 && !viewModel.isSolutionMode.value
        if (!livesExhausted) {
            restoreDefaultFeedbackButtonStyles()
        }

        if (deferImmediateCorrectness) {
            binding.layoutFeedbackPanel.visibility = View.VISIBLE
            binding.tvFeedbackTitle.text = getString(R.string.quiz_feedback_answer_saved_title)
            binding.tvFeedbackTitle.setTextColor(quizColor(R.color.quiz_text_grey))
            binding.layoutFeedbackPanel.setCardBackgroundColor(
                android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_feedback_panel_correct))
            )
            binding.tvFeedbackContent.text = explanation

            serverRemainingLives?.let { updateHearts(it.coerceIn(0, 3), forceReset = true) }
            val isFailedByLives = (serverRemainingLives ?: lives) <= 0
            val isLast = (viewModel.currentQuestionIndex.value >= (binding.pbQuizProgress.max - 1))

            if (isFailedByLives && !viewModel.isSolutionMode.value) {
                bindLivesExhaustedFeedbackActions(canRevive, reviveCost, gearBalance)
            } else {
                restoreDefaultFeedbackButtonStyles()
                binding.btnNextQuestion.text =
                    if (isLast) getString(R.string.quiz_btn_view_result) else getString(R.string.quiz_btn_next)
                binding.btnNextQuestion.setOnClickListener {
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    restoreDefaultFeedbackButtonStyles()
                    viewModel.nextQuestion()
                }
            }
            return
        }

        markCorrectAnswer(question, userAnswer, isCorrect)

        binding.layoutFeedbackPanel.visibility = View.VISIBLE

        if (!isCorrect && !viewModel.isSolutionMode.value) {
            if (serverRemainingLives != null) {
                updateHearts(serverRemainingLives.coerceIn(0, 3), forceReset = true)
            } else {
                updateHearts(lives - 1)
            }
            shakeView(binding.layoutHearts)
            shakeScreen()
        }

        val effectiveLives = serverRemainingLives ?: lives
        val isFailedByLives = effectiveLives <= 0
        val isLast = (viewModel.currentQuestionIndex.value >= (binding.pbQuizProgress.max - 1))

        if (isFailedByLives && !viewModel.isSolutionMode.value) {
            bindLivesExhaustedFeedbackActions(canRevive, reviveCost, gearBalance)
        } else {
            restoreDefaultFeedbackButtonStyles()
            binding.btnNextQuestion.text = if (isLast) getString(R.string.quiz_btn_view_result) else getString(R.string.quiz_btn_next)
            binding.btnNextQuestion.setOnClickListener {
                binding.layoutFeedbackPanel.visibility = View.GONE
                restoreDefaultFeedbackButtonStyles()
                viewModel.nextQuestion()
            }
        }

        if (isCorrect) {
            binding.tvFeedbackTitle.text = getString(R.string.quiz_feedback_correct_xp)
            binding.tvFeedbackTitle.setTextColor(quizColor(R.color.quiz_mint))
            binding.layoutFeedbackPanel.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_feedback_panel_correct)))
        } else {
            if (userAnswer.isEmpty()) {
                binding.tvFeedbackTitle.text = "시간 초과! ⏱"
            } else {
                binding.tvFeedbackTitle.text = "${getString(R.string.quiz_feedback_wrong)} ${getString(R.string.quiz_feedback_wrong_hint)}"
            }
            binding.tvFeedbackTitle.setTextColor(quizColor(R.color.quiz_red))
            binding.layoutFeedbackPanel.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_feedback_panel_wrong)))
        }
        val correctLine = formatCorrectAnswerLine(question, correctAnswer)
        val exp = explanation.trim()
        binding.tvFeedbackContent.text = listOfNotNull(correctLine, exp.takeIf { it.isNotBlank() })
            .joinToString(separator = "\n\n")
    }

    private fun showResult(result: QuizResult) {
        // 어떤 경로로든 결과로 진입 시, 하단 슬라이드(피드백) 정리
        timer?.cancel()
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.btnFeedbackRefillHearts.visibility = View.GONE
        binding.cardReviewBadge.visibility = View.GONE

        binding.layoutQuizResult.visibility = View.VISIBLE
        binding.layoutQuizResult.bringToFront()
        
        // 펫 진화 축하 연출
        if (result.petEvolved) {
            showEvolutionCelebration()
        }

        binding.lavResultPet.setAnimation(R.raw.pet_idle)
        binding.lavResultPet.playAnimation()

        val isReviewSubmit = result.mode == "review" || viewModel.isReviewMode.value

        val totalCount = result.total.takeIf { it > 0 } ?: result.results.size
        val correctCount = result.results.count { it.isCorrect }

        val remainingHearts = viewModel.sessionLives.value.coerceIn(0, 3)
        val heartsLost = maxOf(
            totalHeartsLost,
            (heartsAtQuizStart - remainingHearts).coerceAtLeast(0)
        )
        val grade = if (isReviewSubmit) {
            null
        } else {
            MissionHeartGrade.fromHeartsLost(heartsLost)
        }
        val missionCleared = grade != MissionHeartGrade.FAIL

        binding.tvResultStatus.text = when {
            isReviewSubmit -> "복습 완료!"
            grade == MissionHeartGrade.FAIL -> getString(R.string.quiz_result_fail)
            grade == MissionHeartGrade.PERFECT -> getString(R.string.quiz_result_perfect)
            else -> getString(R.string.quiz_result_success)
        }
        binding.tvResultStatus.setTextColor(
            if (missionCleared || isReviewSubmit) quizColor(R.color.quiz_mint)
            else quizColor(R.color.quiz_red)
        )

        val baseSub = when {
            isReviewSubmit && !result.isPassed ->
                getString(R.string.quiz_result_review_subtitle_fail)
            isReviewSubmit -> getString(R.string.quiz_result_review_subtitle_pass)
            grade == MissionHeartGrade.FAIL -> "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
            grade == MissionHeartGrade.PERFECT -> getString(R.string.quiz_result_perfect_subtitle)
            grade == MissionHeartGrade.SUCCESS -> getString(R.string.quiz_result_success_subtitle)
            else -> "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
        }
        binding.tvResultSub.text = if (result.isFirstClear && missionCleared && !isReviewSubmit) {
            "${getString(R.string.quiz_first_clear_badge)}\n$baseSub"
        } else {
            baseSub
        }

        binding.tvResCorrectCount.text = "$correctCount / $totalCount"
        binding.tvResPassStatus.text = when {
            isReviewSubmit -> getString(R.string.quiz_grade_success)
            grade == MissionHeartGrade.PERFECT -> getString(R.string.quiz_grade_perfect)
            grade == MissionHeartGrade.SUCCESS -> getString(R.string.quiz_grade_success)
            else -> getString(R.string.quiz_grade_fail)
        }
        binding.tvResPassStatus.setTextColor(
            when (grade) {
                MissionHeartGrade.PERFECT, MissionHeartGrade.SUCCESS ->
                    quizColor(R.color.quiz_mint)
                MissionHeartGrade.FAIL -> quizColor(R.color.quiz_red)
                null -> quizColor(R.color.quiz_mint)
            }
        )

        binding.layoutWrongStat.visibility = View.GONE
        binding.layoutStatsContainer.weightSum = 3f
        if (!missionCleared && !isReviewSubmit) {
            binding.btnResRetry.visibility = View.VISIBLE
            binding.btnResFinish.text = "다음에 하기"
            binding.tvResPetBonus.setTextColor(quizColor(R.color.quiz_text_grey))
        } else {
            binding.btnResRetry.visibility = View.GONE
            binding.btnResFinish.text = "학습 완료"
            binding.tvResPetBonus.setTextColor(quizColor(R.color.quiz_hint_gold))
        }

        binding.tvResPetBonus.text = when {
            isReviewSubmit && result.xpEarned > 0 ->
                getString(R.string.quiz_bonus_review_xp, result.xpEarned)
            isReviewSubmit -> getString(R.string.quiz_bonus_review_no_xp)
            result.equippedPetGrade != null && result.bonusXp > 0 ->
                getString(R.string.quiz_bonus_pet_grade, result.equippedPetGrade, result.bonusXp)
            result.bonusXp > 0 -> "+${result.bonusXp} XP"
            else -> "+0% XP"
        }
        
        // XP 애니메이션
        animateXpGain(result.xpEarned, result.currentXp, result.nextLevelXp, result.currentLevel)

        // 스트릭·티켓 (remainingTickets는 서버 스냅샷)
        val streakLine = if (result.streakDays > 0) {
            "🔥 ${result.streakDays}일 연속 스트릭 유지 중!"
        } else null
        val ticketLine = result.remainingTickets?.let {
            getString(
                R.string.quiz_remaining_tickets_line,
                it.normal,
                it.rare,
                it.epic
            )
        }
        val streakBlock = listOfNotNull(streakLine, ticketLine).joinToString("\n")
        if (streakBlock.isNotEmpty()) {
            binding.tvStreakInfo.visibility = View.VISIBLE
            binding.tvStreakInfo.text = streakBlock
        } else {
            binding.tvStreakInfo.visibility = View.GONE
        }

        val displayRewards = result.rewards.filter { reward ->
            if (isReviewSubmit || !missionCleared) reward.type.uppercase() != "GEAR" else true
        }
        binding.layoutRewardsRow.visibility =
            if (missionCleared && displayRewards.isNotEmpty()) View.VISIBLE else View.GONE
        binding.layoutRewardsContainer.removeAllViews()
        displayRewards.forEach { reward ->
            addRewardIcon(reward)
        }

        binding.tvWrongCount.text = "오답: ${result.wrongCount}개"
    }

    private fun animateXpGain(gainedXp: Int, currentXp: Int, maxXp: Int, level: Int) {
        binding.tvResXpGain.text = "+$gainedXp XP"
        val safeMax = maxXp.coerceAtLeast(1)
        if (gainedXp <= 0) {
            binding.pbResXpProgress.max = safeMax
            binding.pbResXpProgress.progress = currentXp.coerceIn(0, safeMax)
            binding.tvResXpStatus.text = "LV.$level (${currentXp.coerceIn(0, safeMax)} / $safeMax)"
            return
        }
        val endXp = if (currentXp >= gainedXp) currentXp else gainedXp
        val startXp = (endXp - gainedXp).coerceAtLeast(0)
        ValueAnimator.ofInt(startXp, endXp).apply {
            duration = 1500
            addUpdateListener {
                val value = it.animatedValue as Int
                binding.pbResXpProgress.progress = value
                binding.pbResXpProgress.max = safeMax
                binding.tvResXpStatus.text = "LV.$level ($value / $safeMax)"
            }
            start()
        }
    }

    private fun getBonusReasonText(reason: String?): String {
        return when (reason) {
            "PET_RARITY_BONUS" -> "펫 등급 보너스"
            "PERFECT_BONUS" -> "퍼펙트 보너스"
            "PARTNER_BONUS" -> "파트너 동행 보너스"
            "STREAK_BONUS" -> "연속 스트릭 보너스"
            else -> reason ?: ""
        }
    }

    private fun bindTermHints(hints: List<TermHint>) {
        binding.chipGroupTermHints.removeAllViews()
        if (hints.isEmpty()) {
            binding.chipGroupTermHints.visibility = View.GONE
            return
        }
        binding.chipGroupTermHints.visibility = View.VISIBLE
        hints.forEach { hint ->
            val chip = Chip(requireContext()).apply {
                text = hint.term
                isClickable = true
                isCheckable = false
                chipBackgroundColor = ColorStateList.valueOf(quizColor(R.color.quiz_button_secondary_bg))
                setTextColor(quizColor(R.color.quiz_hint_gold))
                setOnClickListener { showTermHintSheet(hint) }
            }
            binding.chipGroupTermHints.addView(chip)
        }
    }

    private fun showTermHintSheet(hint: TermHint) {
        val sheet = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.bottom_sheet_term_hint, null, false)
        content.findViewById<TextView>(R.id.tv_term_title).text = hint.term
        content.findViewById<TextView>(R.id.tv_term_description).text = hint.description
        sheet.setContentView(content)
        sheet.show()
    }

    private fun addRewardIcon(reward: QuizReward) {
        val density = resources.displayMetrics.density
        when (reward.type.uppercase()) {
            "GEAR" -> {
                val tv = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                    }
                    text = getString(R.string.quiz_reward_gear_fmt, reward.count)
                    setTextColor(quizColor(R.color.quiz_hint_gold))
                    textSize = 14f
                }
                binding.layoutRewardsContainer.addView(tv)
            }
            else -> {
                val imageView = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()).apply {
                        setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                    }
                    val resId = when (reward.ticketType) {
                        "RARE" -> R.drawable.ic_star_filled
                        "EPIC" -> R.drawable.ic_star_filled
                        else -> R.drawable.ic_star_filled
                    }
                    setImageResource(resId)
                }
                binding.layoutRewardsContainer.addView(imageView)
            }
        }
    }

    private fun updateQuestion(index: Int) {
        val state = viewModel.uiState.value
        if (state is QuizUiState.QuestionLoaded) {
            val total = state.quizQuestions.questions.size
            val question = state.quizQuestions.questions.getOrNull(index)
                ?: run {
                    Log.e("QuizFragment", "updateQuestion out of bounds: index=$index total=$total")
                    return
                }
            binding.tvQuestionCount.text = "${index + 1} / $total 문제"
            bindQuestionDifficultyStars(question.difficulty)

            // v1.3 + v2.3: 유형 라벨 포함 및 하이라이트 적용
            val typeLabel = when(question.type) {
                QuestionType.OX -> "OX 퀴즈"
                QuestionType.MULTIPLE -> "객관식"
                QuestionType.FILL -> "단어 채우기"
                QuestionType.SITUATION -> "상황 판단"
            }
            val fullText = "[$typeLabel] ${question.question}"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            binding.tvQuizQuestion.setTextColor(quizColor(R.color.quiz_text_primary))
            bindTermHints(question.termHints)

            binding.layoutHearts.visibility = View.VISIBLE
            if (index == 0) {
                heartsAtQuizStart = 3
                totalHeartsLost = 0
                updateHearts(3, forceReset = true)
            }
            updateQuizModeBanner()
            setupOptions(question)
            // v2.4: 이미 답한 문항(복구)이라면 입력을 막는다.
            if (viewModel.isAnsweredAlready(question.id)) {
                lockOptions()
                binding.tvAttemptRecoverBanner.visibility = View.VISIBLE
                binding.tvAttemptRecoverBanner.text = getString(R.string.quiz_attempt_recovered_banner)
                binding.tvTimer.text = getString(R.string.quiz_timer_recovered_label)
                binding.tvTimer.setTextColor(quizColor(R.color.quiz_text_grey))
                timer?.cancel()
                timer = null
                // 복구된 문항에서는 선택 대신 "다음"으로 진행할 수 있게 보장
                binding.btnNextQuestion.text = getString(R.string.quiz_btn_next)
                binding.btnNextQuestion.setOnClickListener {
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    binding.btnFeedbackRefillHearts.visibility = View.GONE
                    viewModel.nextQuestion()
                }
            } else {
                binding.tvAttemptRecoverBanner.visibility = View.GONE
                if (!viewModel.isSolutionMode.value) {
                    startTimer(reset = true)
                }
            }
        }
    }

    /** 재도전(strict) 시 제목 아래 안내. 복습 미션이면 '복습 · 다시 도전하기'. */
    private fun updateQuizModeBanner() {
        val strict = viewModel.strictSingleLifeRetry.value
        val review = viewModel.isReviewMode.value
        val banner = binding.tvQuizModeBanner
        when {
            strict && review -> {
                banner.text = getString(R.string.quiz_mode_review_and_retry)
                banner.visibility = View.VISIBLE
            }
            strict -> {
                banner.text = getString(R.string.quiz_mode_retry_challenge)
                banner.visibility = View.VISIBLE
            }
            else -> banner.visibility = View.GONE
        }
    }

    private fun setHighlightedText(view: TextView, text: String) {
        val spannable = SpannableString(text)
        val highlightColor = quizColor(R.color.quiz_mint)
        val quoteColor = quizColor(R.color.quiz_hint_gold)

        // 1. 대괄호 [ ... ] 하이라이트 (Mint)
        var bStart = text.indexOf("[")
        while (bStart != -1) {
            val bEnd = text.indexOf("]", bStart + 1)
            if (bEnd != -1) {
                spannable.setSpan(
                    ForegroundColorSpan(highlightColor),
                    bStart,
                    bEnd + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                bStart = text.indexOf("[", bEnd + 1)
            } else {
                break
            }
        }

        // 2. 따옴표 " ... " 하이라이트 (Yellow)
        var qStart = text.indexOf("\"")
        while (qStart != -1) {
            val qEnd = text.indexOf("\"", qStart + 1)
            if (qEnd != -1) {
                spannable.setSpan(
                    ForegroundColorSpan(quoteColor),
                    qStart,
                    qEnd + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                qStart = text.indexOf("\"", qEnd + 1)
            } else {
                break
            }
        }
        view.text = spannable
    }

    private fun updateHearts(newLives: Int, forceReset: Boolean = false) {
        val hearts = listOf(binding.ivHeart1, binding.ivHeart2, binding.ivHeart3)
        val capped = newLives.coerceIn(0, 3)
        if (capped < lives) {
            totalHeartsLost += (lives - capped)
        }
        if (!forceReset && capped < lives) {
            shakeView(binding.layoutHearts)
        }
        hearts.forEachIndexed { index, imageView ->
            val isFilled = index < capped
            imageView.setImageResource(if (isFilled) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty)
            imageView.scaleX = 1f
            imageView.scaleY = 1f
        }
        lives = capped
    }

    private fun shakeView(v: View) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, 10f).apply {
            duration = 500
            interpolator = CycleInterpolator(3f)
            start()
        }
    }

    private fun shakeScreen() {
        // 루트를 흔들면 화면 밖 배경(흰색)이 비칠 수 있어, 컨텐츠만 함께 흔든다.
        val shakeCard = ObjectAnimator.ofFloat(binding.layoutQuestionCard, "translationX", 0f, 10f).apply {
            duration = 420
            interpolator = CycleInterpolator(4f)
        }
        val shakeOptions = ObjectAnimator.ofFloat(binding.layoutOptionsContainer, "translationX", 0f, 10f).apply {
            duration = 420
            interpolator = CycleInterpolator(4f)
        }
        shakeCard.start()
        shakeOptions.start()
    }

    private fun setupOptions(question: Question) {
        binding.layoutOptionsChips.removeAllViews()
        
        // 초기화
        binding.layoutOptionsStandard.visibility = View.GONE
        binding.layoutOptionsOx.visibility = View.GONE
        binding.layoutOptionsChips.visibility = View.GONE
        // OX는 이전 문항/모드에서 스트로크가 남을 수 있어 항상 기본값으로 초기화
        resetOxButtons()
        resetMultipleFixedOptions()
        // 다음 문항을 위해 객관식/칩 클릭 가능 상태 복구
        unlockOptionsForNewQuestion()

        when (question.type) {
            QuestionType.OX -> {
                binding.layoutOptionsOx.visibility = View.VISIBLE
            }
            else -> {
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                setupMultipleFixedOptions(question)
            }
        }
    }

    private fun unlockOptionsForNewQuestion() {
        // 객관식 고정 카드 클릭 가능 복구
        listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        ).forEach { card ->
            card.isEnabled = true
            card.isClickable = true
            card.alpha = 1f
        }

        // OX도 클릭 가능 복구
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        binding.btnOxO.isClickable = true
        binding.btnOxX.isClickable = true
        binding.btnOxO.alpha = 1f
        binding.btnOxX.alpha = 1f
    }

    private fun setupMultipleFixedOptions(question: Question) {
        val options = question.options.orEmpty()
        val cards = listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        )
        val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)

        cards.forEachIndexed { idx, card ->
            val option = options.getOrNull(idx)
            if (option == null) {
                card.visibility = View.GONE
            } else {
                card.visibility = View.VISIBLE
                texts[idx].text = option
                texts[idx].maxLines = Integer.MAX_VALUE
                card.isEnabled = true
                card.isClickable = true
                card.alpha = 1f
                card.setOnClickListener {
                    resetMultipleFixedOptions()
                    applyMultipleFixedSelectionByIndex(idx)
                    handleOptionClick(
                        (idx + 1).toString(),
                        fillDisplayWord = if (question.type == QuestionType.FILL) option else null,
                    )
                }
            }
        }
    }

    private fun resetMultipleFixedOptions() {
        val cards = listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        )
        val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        val checks = listOf(binding.ivCheck1, binding.ivCheck2, binding.ivCheck3, binding.ivCheck4)

        cards.forEachIndexed { idx, card ->
            card.isEnabled = true
            card.isClickable = true
            card.alpha = 1f
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_card_bg))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = quizColor(R.color.quiz_option_default_stroke)
            texts[idx].isEnabled = true
            texts[idx].alpha = 1f
            texts[idx].setTextColor(quizColor(R.color.quiz_text_primary))
            checks[idx].visibility = View.GONE
        }
    }

    private fun resetOxButtons() {
        val density = resources.displayMetrics.density
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        binding.btnOxO.isClickable = true
        binding.btnOxX.isClickable = true
        binding.btnOxO.alpha = 1f
        binding.btnOxX.alpha = 1f
        binding.btnOxO.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_card_bg))
        binding.btnOxX.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_card_bg))
        binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_option_default_stroke)))
        binding.btnOxO.setStrokeWidth((1 * density).toInt())
        binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_option_default_stroke)))
        binding.btnOxX.setStrokeWidth((1 * density).toInt())
        // applyOxPendingSelection()에서 바꾼 O/X 라벨 색 — 다음 OX 문항으로 넘어갈 때 잔상 제거
        binding.tvOxOIcon.setTextColor(quizColor(R.color.quiz_mint))
        binding.tvOxOText.setTextColor(quizColor(R.color.quiz_mint))
        binding.tvOxXIcon.setTextColor(quizColor(R.color.quiz_red))
        binding.tvOxXText.setTextColor(quizColor(R.color.quiz_red))
    }

    /** OX 탭 직후(채점 전) '내 선택'을 바로 표시 */
    private fun applyOxPendingSelection(choice: String) {
        val density = resources.displayMetrics.density

        // 기본값으로 리셋 후, '내가 고른 보기'는 전면 민트로 통일 (체크 아이콘/스트로크 강조 대신)
        resetOxButtons()

        val mint = ContextCompat.getColor(requireContext(), R.color.quiz_mint)
        val selected = if (choice == "O") binding.btnOxO else binding.btnOxX
        selected.setCardBackgroundColor(mint)
        selected.setStrokeColor(android.content.res.ColorStateList.valueOf(mint))
        selected.setStrokeWidth((2 * density).toInt())

        // 텍스트 가독성 보강
        val oIcon = binding.tvOxOIcon
        val oText = binding.tvOxOText
        val xIcon = binding.tvOxXIcon
        val xText = binding.tvOxXText

        val selectedTextColor = Color.WHITE
        val defaultTextColor = ContextCompat.getColor(requireContext(), R.color.quiz_mint)
        if (choice == "O") {
            oIcon.setTextColor(selectedTextColor)
            oText.setTextColor(selectedTextColor)
            xIcon.setTextColor(defaultTextColor)
            xText.setTextColor(defaultTextColor)
        } else {
            xIcon.setTextColor(selectedTextColor)
            xText.setTextColor(selectedTextColor)
            oIcon.setTextColor(defaultTextColor)
            oText.setTextColor(defaultTextColor)
        }
    }

    /** 문항 난이도 → 문제 카드 상단 별 1~3개 (쉬움·보통·어려움) */
    private fun bindQuestionDifficultyStars(difficulty: QuestionDifficulty?) {
        val stars = listOf(
            binding.ivQuestionDiffStar1,
            binding.ivQuestionDiffStar2,
            binding.ivQuestionDiffStar3,
        )
        val filledCount = when (difficulty) {
            QuestionDifficulty.LOW -> 1
            QuestionDifficulty.MEDIUM -> 2
            QuestionDifficulty.HIGH -> 3
            null -> 0
        }
        if (filledCount == 0) {
            binding.layoutQuestionDifficultyStars.visibility = View.GONE
            return
        }
        binding.layoutQuestionDifficultyStars.visibility = View.VISIBLE
        stars.forEachIndexed { index, star ->
            star.visibility = if (index < filledCount) View.VISIBLE else View.GONE
        }
    }

    private fun animateSelection(view: View) {
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            .start()
    }

    private fun handleOptionClick(answerForServer: String, fillDisplayWord: String? = null) {
        timer?.cancel()
        val question = viewModel.getCurrentCachedQuestion() ?: getCurrentQuestion() ?: return
        
        val currentIndex = viewModel.currentQuestionIndex.value
        if (currentIndex > maxPlayedIndex) {
            maxPlayedIndex = currentIndex
        }
        
        // FILL 유형 시각적 피드백: 빈칸 채우기 (v2.3 명세 준수 - 즉시 치환)
        if (question.type == QuestionType.FILL) {
            val typeLabel = "단어 채우기"
            val originalText = question.question
            val display = fillDisplayWord ?: answerForServer
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $display ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $display ")
                else -> originalText
            }
            val fullText = "[$typeLabel] $replacedText"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            binding.tvQuizQuestion.setTextColor(quizColor(R.color.quiz_text_primary))
        }

        lockOptions()
        
        if (!viewModel.isSolutionMode.value) {
            viewModel.checkAnswer(question.id, answerForServer)
        } else {
            viewModel.selectAnswer(question.id, answerForServer)
        }
    }

    private fun getCurrentQuestion(): Question? {
        val state = viewModel.uiState.value
        return if (state is QuizUiState.QuestionLoaded) {
            state.quizQuestions.questions.getOrNull(viewModel.currentQuestionIndex.value)
        } else null
    }

    private fun lockOptions() {
        // 피드백/풀이 화면에서는 클릭만 막고(회색 비활성화 금지) 다음 문항에서 unlock로 복구한다.
        binding.btnOxO.isClickable = false
        binding.btnOxX.isClickable = false

        // 고정 객관식 카드 클릭만 차단
        listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        ).forEach { card ->
            card.isClickable = false
            card.alpha = 1f
        }

    }

    private fun showFeedback(title: String, content: String) {
        binding.layoutQuizResult.visibility = View.GONE
        binding.btnFeedbackRefillHearts.visibility = View.GONE
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = title
        binding.tvFeedbackTitle.setTextColor(quizColor(R.color.quiz_text_primary))
        binding.tvFeedbackContent.text = content
        binding.tvFeedbackContent.setTextColor(quizColor(R.color.quiz_feedback_text))
        binding.layoutFeedbackPanel.setCardBackgroundColor(
            android.content.res.ColorStateList.valueOf(quizColor(R.color.quiz_card_bg))
        )
    }

    private fun showEvolutionCelebration() {
        try {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_evolution, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_evolution_close)
            btnClose?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            
            val lavEvolution = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lav_evolution_effect)
            // 실제 진화 애니메이션이 있으면 교체, 없으면 기본 아이들링 유지
            // lavEvolution.setAnimation("pet_evolution.json")
            lavEvolution.playAnimation()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "축하합니다! 아이몽이 진화했습니다!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showMissionSetNotReadyDialog() {
        try {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mission_not_ready, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_mission_not_ready_close)
            btnClose?.setOnClickListener {
                dialog.dismiss()
                findNavController().popBackStack()
            }

            dialog.show()
            
            val lavEffect = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lav_mission_not_ready_effect)
            lavEffect?.playAnimation()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "문제 세트를 준비하는 데 실패했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    companion object {
        /** 목업 홈([MockHomeFragment]) 전용 — 실 API는 [com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus] */
        const val REQUEST_QUIZ_FINISHED = "quiz_finished"
        const val EXTRA_REFRESH_HOME = "refresh_home"
    }
}
