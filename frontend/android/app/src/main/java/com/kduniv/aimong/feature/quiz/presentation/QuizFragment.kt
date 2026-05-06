package com.kduniv.aimong.feature.quiz.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentQuizBinding
import com.kduniv.aimong.feature.quiz.domain.model.Question
import com.kduniv.aimong.feature.quiz.domain.model.QuestionType
import com.kduniv.aimong.feature.quiz.domain.model.QuizResult
import com.kduniv.aimong.feature.quiz.domain.model.QuizReward
import com.kduniv.aimong.feature.quiz.domain.model.QuizQuestions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.drawable.GradientDrawable

@AndroidEntryPoint
class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()

    private var lives = 3
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
                    binding.tvTimer.setTextColor(Color.parseColor("#8A96AD"))
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
            findNavController().popBackStack()
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
            maxPlayedIndex = 0

            binding.layoutFeedbackPanel.visibility = View.GONE
            binding.layoutQuizResult.visibility = View.GONE
            unlockOptionsForNewQuestion()
            resetOxButtons()
            resetMultipleFixedOptions()

            binding.btnResRetry.isEnabled = false
            binding.btnResRetry.alpha = 0.5f
            viewModel.retryQuiz()
        }
        binding.btnResFinish.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnNextQuestion.setOnClickListener {
            // 결과 화면에서 "다시하기"를 누른 직후(두 번째 텀 로딩 중)엔 이전 텀의 피드백/전이와 충돌하지 않게 막는다.
            if (isRetryingFromResult) return@setOnClickListener
            binding.layoutFeedbackPanel.visibility = View.GONE
            viewModel.nextQuestion()
        }
        binding.btnFeedbackRetry.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
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
                            binding.tvExpInfo.setTextColor(Color.parseColor("#FFD600"))
                        }
                        updateQuizModeBanner()
                    }
                }
                launch {
                    viewModel.isSolutionMode.collect { isSolution ->
                        if (isSolution) {
                            // 풀이 모드에서는 문항 30초 타이머 정지 + 잔상 제거
                            timer?.cancel()
                            binding.tvTimer.text = "풀이 보기"
                            binding.tvTimer.setTextColor(Color.parseColor("#8A96AD"))
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
                updateQuestion(viewModel.currentQuestionIndex.value)
            }
            is QuizUiState.AnswerChecked -> {
                val q =
                    viewModel.getCurrentCachedQuestion()
                        ?: viewModel.getCachedQuestionAt(viewModel.currentQuestionIndex.value)
                if (q != null) {
                    showAnswerFeedback(q, state.isCorrect, state.explanation, state.userAnswer)
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
        binding.tvQuizQuestion.text = state.question.question
        binding.pbQuizProgress.progress = index + 1
        
        setupOptions(state.question)
        // 풀이 모드에서는 클릭 방지 및 정답 표시
        lockOptions()
        markCorrectAnswer(state.question, state.userAnswer, state.isCorrect)
        
        showAnswerFeedback(state.question, state.isCorrect, state.explanation, state.userAnswer)
        binding.btnFeedbackRetry.visibility = View.GONE // 풀이 모드에선 다시보기 불필요
        
        val targetSize = if (lives <= 0) maxPlayedIndex else total - 1
        val isLast = index >= targetSize
        binding.btnNextQuestion.text = if (isLast) "결과로 돌아가기" else "다음 풀이 →"
        
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
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
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $userAnswer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $userAnswer ")
                else -> originalText
            }
            val fullText = "[$typeLabel] $replacedText"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            // 문제 본문 색은 정오와 무관하게 고정(옵션 영역에서만 정오 표시)
            binding.tvQuizQuestion.setTextColor(Color.WHITE)
        }

        // OX 유형
        if (question.type == QuestionType.OX) {
            // 이전 문항/모드의 스트로크 잔상 제거 후 정오 표시
            resetOxButtons()
            val correctLabel = if (isCorrect) userAnswer else (if (userAnswer == "O") "X" else "O")
            if (correctLabel == "O") {
                binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                binding.btnOxO.setStrokeWidth((8 * density).toInt())
            } else {
                binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                binding.btnOxX.setStrokeWidth((8 * density).toInt())
            }
            
            if (!isCorrect) {
                if (userAnswer == "O") {
                    binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                    binding.btnOxO.setStrokeWidth((8 * density).toInt())
                } else {
                    binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                    binding.btnOxX.setStrokeWidth((8 * density).toInt())
                }
            }
        } 
        // 객관식(MULTIPLE): 내가 고른 보기만 민트로 표시
        else if (question.type == QuestionType.MULTIPLE) {
            val selectedKey = userAnswer.trim()
            // XML 고정 카드 기반 표시
            applyMultipleFixedSelection(selectedKey)
        }
        // Chip 유형 (FILL, SITUATION)
        else {
            for (i in 0 until binding.layoutOptionsChips.childCount) {
                val chip = binding.layoutOptionsChips.getChildAt(i) as? Chip ?: continue
                if (chip.text == userAnswer) {
                    // 풀이 모드에서는 정오와 무관하게 '내가 고른 보기'만 민트로 통일
                    if (isSolutionMode || isCorrect) {
                        chip.setChipBackgroundColorResource(R.color.quiz_mint)
                        chip.setTextColor(Color.parseColor("#0A1633"))
                        chip.chipStrokeWidth = 0f
                    } else {
                        chip.setChipBackgroundColorResource(R.color.quiz_red)
                        chip.setTextColor(Color.WHITE)
                    }
                }
            }
        }
    }

    private fun showAnswerFeedback(question: Question, isCorrect: Boolean, explanation: String, userAnswer: String) {
        markCorrectAnswer(question, userAnswer, isCorrect)

        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        
        if (!isCorrect) {
            // 풀이 모드에서는 패널티/흔들림/하트 감소 없음
            if (!viewModel.isSolutionMode.value) {
                // lives를 먼저 줄이면 updateHearts의 감소 감지가 깨지므로, 현재 lives 기준으로 감소 처리
                updateHearts(lives - 1)
                // 하트 감소가 체감되도록 하트 영역도 한번 더 흔들림 트리거
                shakeView(binding.layoutHearts)
                shakeScreen()
            }
        }

        // 마지막 문제인 경우 또는 라이프가 0인 경우 버튼 텍스트 변경
        val questions = viewModel.uiState.value.let { 
            if (it is QuizUiState.QuestionLoaded) it.quizQuestions.questions 
            else (viewModel.uiState.value as? QuizUiState.AnswerChecked)?.let { 
                // 이 시점에는 QuestionLoaded 정보가 캐시되어 있어야 함
                null // 실제로는 캐시된 정보를 쓰거나 ViewModel에서 확인 필요
            }
        }
        
        val isFailedByLives = lives <= 0
        val isLast = (viewModel.currentQuestionIndex.value >= (binding.pbQuizProgress.max - 1))
        
        if (isFailedByLives) {
            binding.btnNextQuestion.text = getString(R.string.quiz_btn_view_result)
            binding.btnNextQuestion.setOnClickListener {
                binding.layoutFeedbackPanel.visibility = View.GONE
                // 복습(하트 1개) 오답으로 실패했을 때도 피드백 패널은 보여주고, 버튼으로 결과로 이동
                if (viewModel.isReviewMode.value && !viewModel.isSolutionMode.value) {
                    viewModel.finishReviewImmediatelyOnWrong(explanation)
                } else {
                    viewModel.finishQuizEarly()
                }
            }
        } else {
            binding.btnNextQuestion.text = if (isLast) getString(R.string.quiz_btn_view_result) else getString(R.string.quiz_btn_next)
            binding.btnNextQuestion.setOnClickListener {
                binding.layoutFeedbackPanel.visibility = View.GONE
                viewModel.nextQuestion()
            }
        }

        if (isCorrect) {
            binding.tvFeedbackTitle.text = getString(R.string.quiz_feedback_correct_xp)
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#00FFB2"))
            binding.layoutFeedbackPanel.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D1D41")))
        } else {
            if (userAnswer.isEmpty()) {
                binding.tvFeedbackTitle.text = "시간 초과! ⏱"
            } else {
                binding.tvFeedbackTitle.text = "${getString(R.string.quiz_feedback_wrong)} ${getString(R.string.quiz_feedback_wrong_hint)}"
            }
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
            binding.layoutFeedbackPanel.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1025")))
        }
        binding.tvFeedbackContent.text = explanation
    }

    private fun showResult(result: QuizResult) {
        // 어떤 경로로든 결과로 진입 시, 하단 슬라이드(피드백) 정리
        timer?.cancel()
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.cardReviewBadge.visibility = View.GONE

        binding.layoutQuizResult.visibility = View.VISIBLE
        binding.layoutQuizResult.bringToFront()
        
        // 펫 진화 축하 연출
        if (result.petEvolved) {
            showEvolutionCelebration()
        }

        binding.lavResultPet.setAnimation(R.raw.pet_idle)
        binding.lavResultPet.playAnimation()

        val isReviewSubmit = result.mode == "review"

        val totalCount = result.total.takeIf { it > 0 } ?: result.results.size
        val correctCount = result.results.count { it.isCorrect }
        val wrongCount = (result.results.size - correctCount).coerceAtLeast(0)

        // 서버/클라 지표를 함께 사용해 PASS/FAIL을 안전하게 결정
        // - server veto: isPassed=false 또는 wrongCount>0 이면 무조건 FAIL
        // - results 기반 교차검증: 모든 문항 정답이어야 PASS
        val uiPassed =
            result.isPassed &&
                (result.wrongCount == 0) &&
                (wrongCount == 0) &&
                (correctCount == totalCount)

        binding.tvResultStatus.text = when {
            !uiPassed -> getString(R.string.quiz_result_fail)
            isReviewSubmit -> "복습 완료!"
            else -> getString(R.string.quiz_result_success)
        }
        binding.tvResultStatus.setTextColor(
            if (uiPassed) Color.parseColor("#00FFB2")
            else Color.parseColor("#FF4B4B")
        )

        binding.tvResultSub.text = when {
            !uiPassed -> "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
            isReviewSubmit -> getString(R.string.quiz_result_review_subtitle_pass)
            else -> "정말 대단해! 리터러시 박사가 다 됐는걸?"
        }
        
        binding.tvResCorrectCount.text = "$correctCount / $totalCount"
        binding.tvResPassStatus.text = if (uiPassed) "PASS" else "FAIL"
        binding.tvResPassStatus.setTextColor(
            if (uiPassed) Color.parseColor("#00FFB2")
            else Color.parseColor("#FF4B4B")
        )

        // 오답 통계 표시
        // 오답 수는 결과 화면에서 노출하지 않음
        binding.layoutWrongStat.visibility = View.GONE
        binding.layoutStatsContainer.weightSum = 3f
        if (!uiPassed) {
            binding.btnResRetry.visibility = View.VISIBLE
            binding.btnResFinish.text = "다음에 하기"
            binding.tvResPetBonus.setTextColor(Color.parseColor("#8A96AD"))
        } else {
            binding.btnResRetry.visibility = View.GONE
            binding.btnResFinish.text = "학습 완료"
            binding.tvResPetBonus.setTextColor(Color.parseColor("#FFD600"))
        }

        // 보너스 정보 (v1.4: 복습은 bonusXp/xpEarned 0, equippedPetGrade 등)
        binding.tvResPetBonus.text = when {
            isReviewSubmit -> getString(R.string.quiz_bonus_review_none)
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

        // 보상 아이템 표시
        binding.layoutRewardsRow.visibility =
            if (result.isPassed && result.rewards.isNotEmpty()) View.VISIBLE else View.GONE
        binding.layoutRewardsContainer.removeAllViews()
        result.rewards.forEach { reward ->
            addRewardIcon(reward)
        }

        if (viewModel.strictSingleLifeRetry.value && !result.isPassed) {
            updateHearts(0, forceReset = true)
        }

        binding.tvWrongCount.text = "오답: ${result.wrongCount}개"
    }

    private fun animateXpGain(gainedXp: Int, currentXp: Int, maxXp: Int, level: Int) {
        val startXp = (currentXp - gainedXp).coerceAtLeast(0)
        binding.tvResXpGain.text = "+$gainedXp XP"
        
        ValueAnimator.ofInt(startXp, currentXp).apply {
            duration = 1500
            addUpdateListener { 
                val value = it.animatedValue as Int
                binding.pbResXpProgress.progress = value
                binding.pbResXpProgress.max = maxXp
                binding.tvResXpStatus.text = "LV.$level ($value / $maxXp)"
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

    private fun addRewardIcon(reward: QuizReward) {
        val density = resources.displayMetrics.density
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()).apply {
                setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            // 리워드 타입에 따른 이미지 설정
            // TODO: 티켓 등급별 리소스(ic_ticket_rare 등) 추가 필요. 우선 기본 별 아이콘 사용
            val resId = when (reward.ticketType) {
                "RARE" -> R.drawable.ic_star_filled
                "EPIC" -> R.drawable.ic_star_filled
                else -> R.drawable.ic_star_filled
            }
            setImageResource(resId)
        }
        binding.layoutRewardsContainer.addView(imageView)
    }

    private fun updateQuestion(index: Int) {
        val state = viewModel.uiState.value
        if (state is QuizUiState.QuestionLoaded) {
            val total = state.quizQuestions.questions.size
            binding.tvQuestionCount.text = "${index + 1} / $total 문제"
            
            val question = state.quizQuestions.questions.getOrNull(index)
                ?: run {
                    Log.e("QuizFragment", "updateQuestion out of bounds: index=$index total=$total")
                    return
                }
            
            // v1.3 + v2.3: 유형 라벨 포함 및 하이라이트 적용
            val typeLabel = when(question.type) {
                QuestionType.OX -> "OX 퀴즈"
                QuestionType.MULTIPLE -> "객관식"
                QuestionType.FILL -> "단어 채우기"
                QuestionType.SITUATION -> "상황 판단"
            }
            val fullText = "[$typeLabel] ${question.question}"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            binding.tvQuizQuestion.setTextColor(Color.WHITE)

            binding.layoutHearts.visibility = View.VISIBLE
            // 복습 모드도 하트 1개로 시작
            val maxLives = if (viewModel.isReviewMode.value || viewModel.strictSingleLifeRetry.value) 1 else 3
            if (index == 0) updateHearts(maxLives, forceReset = true)
            updateQuizModeBanner()
            setupOptions(question)
            startTimer(reset = true)
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
        val highlightColor = Color.parseColor("#00FFB2")
        val quoteColor = Color.parseColor("#FFD600")

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
            QuestionType.FILL, QuestionType.SITUATION -> {
                binding.layoutOptionsChips.visibility = View.VISIBLE
                setupChips(question)
            }
            else -> {
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                setupMultipleFixedOptions(question.options.orEmpty())
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

        // 칩들도 클릭 가능 복구(생성 후에 다시 잠길 수 있음)
        for (i in 0 until binding.layoutOptionsChips.childCount) {
            val v = binding.layoutOptionsChips.getChildAt(i)
            v.isEnabled = true
            v.isClickable = true
            v.alpha = 1f
        }

        // OX도 클릭 가능 복구
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        binding.btnOxO.isClickable = true
        binding.btnOxX.isClickable = true
        binding.btnOxO.alpha = 1f
        binding.btnOxX.alpha = 1f
    }

    private fun setupMultipleFixedOptions(options: List<String>) {
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
                card.isEnabled = true
                card.isClickable = true
                card.alpha = 1f
                // 선택 스타일은 클릭에서 적용, 기본은 초기화 상태 유지
                card.setOnClickListener {
                    // 단일 선택 리셋
                    resetMultipleFixedOptions()
                    applyMultipleFixedSelection(option.trim())
                    handleOptionClick(option)
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
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.home_card_bg))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = Color.parseColor("#243B70")
            texts[idx].isEnabled = true
            texts[idx].alpha = 1f
            texts[idx].setTextColor(Color.WHITE)
            checks[idx].visibility = View.GONE
        }
    }

    private fun applyMultipleFixedSelection(selectedKey: String) {
        val cards = listOf(
            binding.btnQuizMultOpt1,
            binding.btnQuizMultOpt2,
            binding.btnQuizMultOpt3,
            binding.btnQuizMultOpt4
        )
        val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        // 체크표시는 사용하지 않음
        val density = resources.displayMetrics.density
        cards.forEachIndexed { idx, card ->
            val key = texts[idx].text?.toString()?.trim().orEmpty()
            if (key.isNotEmpty() && key == selectedKey) {
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quiz_mint))
                card.strokeWidth = (3 * density).toInt()
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.quiz_mint)
                texts[idx].setTextColor(Color.WHITE)
            }
        }
    }

    private fun resetOxButtons() {
        val density = resources.displayMetrics.density
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        binding.btnOxO.isClickable = true
        binding.btnOxX.isClickable = true
        binding.btnOxO.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.home_card_bg))
        binding.btnOxX.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.home_card_bg))
        binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70")))
        binding.btnOxO.setStrokeWidth((1 * density).toInt())
        binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70")))
        binding.btnOxX.setStrokeWidth((1 * density).toInt())
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

    private fun setupChips(question: Question) {
        val density = resources.displayMetrics.density
        val isSituation = question.type == QuestionType.SITUATION

        binding.layoutOptionsChips.chipSpacingVertical = (8 * density).toInt()
        binding.layoutOptionsChips.chipSpacingHorizontal = (8 * density).toInt()

        question.options?.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option
                textSize = if (isSituation) 14f else 15f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                isSingleLine = false
                maxLines = 20
                ellipsize = null
                isClickable = true
                isCheckable = false
                checkedIcon = null
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Color.WHITE)
                setEnsureMinTouchTargetSize(false)

                if (isSituation) {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (8 * density).toInt())
                    }
                    minHeight = (60 * density).toInt()
                    chipStartPadding = 20 * density
                    chipEndPadding = 20 * density
                    // SITUATION도 '선택 시 전체 민트'가 명확히 보이도록 Chip 기반 스타일로 통일
                    setChipBackgroundColorResource(R.color.home_card_bg)
                    setChipStrokeColorResource(R.color.home_card_stroke)
                    chipStrokeWidth = 3f * density
                } else {
                    minHeight = (48 * density).toInt()
                    chipStartPadding = 16 * density
                    chipEndPadding = 16 * density
                    setChipBackgroundColorResource(R.color.home_card_bg)
                    setChipStrokeColorResource(R.color.home_card_stroke)
                    chipStrokeWidth = 3f * density
                }

                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(if (isSituation) 16 * density else 28 * density)
                    .build()

                setOnClickListener {
                    // 시각적 피드백 강화 (애니메이션 및 색상 변경)
                    animateSelection(this)
                    
                    // 체크표시 대신 '선택지 전체 민트'로 통일
                    setChipBackgroundColorResource(R.color.quiz_mint)
                    setChipStrokeColorResource(R.color.quiz_mint)
                    setTextColor(Color.WHITE)
                    
                    handleOptionClick(option)
                }
            }
            binding.layoutOptionsChips.addView(chip)
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

    private fun addOptionButton(parent: LinearLayout, text: String, index: Int) {
        val density = resources.displayMetrics.density
        val relativeLayout = RelativeLayout(requireContext()).apply {
            tag = text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            isClickable = true
            isFocusable = true
        }

        val textView = TextView(requireContext()).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            this.text = "${index + 1}  $text"
            setTextColor(Color.WHITE)
            textSize = 13f
            tag = "option_text"
        }

        applyMultipleRowStyle(relativeLayout, isSelected = false)
        relativeLayout.addView(textView)
        
        relativeLayout.setOnClickListener {
            // 단일 선택: 기존 선택 스타일 리셋
            for (i in 0 until parent.childCount) {
                val row = parent.getChildAt(i) as? RelativeLayout ?: continue
                applyMultipleRowStyle(row, isSelected = false)
            }

            // 내가 선택한 보기: 민트로 채우기
            applyMultipleRowStyle(relativeLayout, isSelected = true)
            handleOptionClick(text)
        }

        parent.addView(relativeLayout)
    }

    private fun applyMultipleRowStyle(row: RelativeLayout, isSelected: Boolean) {
        val density = resources.displayMetrics.density
        val radius = 16f * density
        val strokeW = (1f * density).toInt()

        val bgColor = if (isSelected) {
            ContextCompat.getColor(requireContext(), R.color.quiz_mint)
        } else {
            ContextCompat.getColor(requireContext(), R.color.home_card_bg)
        }
        val strokeColor = if (isSelected) {
            ContextCompat.getColor(requireContext(), R.color.quiz_mint)
        } else {
            Color.parseColor("#243B70")
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
            setStroke(strokeW, strokeColor)
        }

        // selector/ripple을 제거하고 고정 배경을 적용
        row.background = drawable

        val tv = row.findViewWithTag<TextView>("option_text")
        if (isSelected) {
            // 민트 배경 위 가독성 우선(회색처럼 보이는 현상 방지)
            tv?.setTextColor(Color.WHITE)
        } else {
            tv?.setTextColor(Color.WHITE)
        }
    }

    private fun handleOptionClick(answer: String) {
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
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $answer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $answer ")
                else -> originalText
            }
            val fullText = "[$typeLabel] $replacedText"
            setHighlightedText(binding.tvQuizQuestion, fullText)
            binding.tvQuizQuestion.setTextColor(Color.WHITE)
        }

        lockOptions()
        
        if (!viewModel.isSolutionMode.value) {
            viewModel.checkAnswer(question.id, answer)
        } else {
            viewModel.selectAnswer(question.id, answer)
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

        for (i in 0 until binding.layoutOptionsChips.childCount) {
            val v = binding.layoutOptionsChips.getChildAt(i)
            v.isClickable = false
            v.alpha = 1f
        }
    }

    private fun showFeedback(title: String, content: String) {
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = title
        binding.tvFeedbackContent.text = content
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
}
