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
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.app.AlertDialog
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

@AndroidEntryPoint
class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()

    private var lives = 3
    private var maxPlayedIndex = 0
    private var timer: CountDownTimer? = null
    private var _isAdded = false

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _isAdded = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _isAdded = false
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTimer.text = "⏱ ${millisUntilFinished / 1000}초 남음"
                if (millisUntilFinished <= 10000) {
                    binding.tvTimer.setTextColor(Color.RED)
                } else {
                    binding.tvTimer.setTextColor(Color.parseColor("#8A96AD"))
                }
            }
            override fun onFinish() {
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
            binding.layoutQuizResult.visibility = View.GONE
            viewModel.retryQuiz()
        }
        binding.btnResFinish.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            viewModel.nextQuestion()
        }
        binding.btnFeedbackRetry.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
        }
        binding.btnOxO.setOnClickListener { 
            animateSelection(it)
            handleOptionClick("O") 
        }
        binding.btnOxX.setOnClickListener { 
            animateSelection(it)
            handleOptionClick("X") 
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
                        binding.cardReviewBadge.visibility = if (isReview) View.VISIBLE else View.GONE
                        if (isReview) {
                            binding.tvExpInfo.text = "복습 시 EXP 50% 획득"
                            binding.tvExpInfo.setTextColor(Color.parseColor("#FFD600"))
                        }
                        updateQuizModeBanner()
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
                updateQuestion(viewModel.currentQuestionIndex.value)
            }
            is QuizUiState.AnswerChecked -> {
                showAnswerFeedback(state.isCorrect, state.explanation, state.userAnswer)
            }
            is QuizUiState.SolutionLoaded -> {
                showSolution(state)
            }
            is QuizUiState.Finished -> {
                showResult(state.result)
            }
            is QuizUiState.Error -> {
                if (state.message == "세션이 만료되었습니다.") {
                    showFeedback("만료", state.message)
                } else {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
        val reasons = listOf(
            "SAFETY" to R.string.quiz_report_reason_safety,
            "INAPPROPRIATE" to R.string.quiz_report_reason_inappropriate,
            "DUPLICATE" to R.string.quiz_report_reason_duplicate,
            "WRONG_ANSWER" to R.string.quiz_report_reason_wrong_answer,
            "LOW_QUALITY" to R.string.quiz_report_reason_low_quality,
            "ETC" to R.string.quiz_report_reason_etc
        )
        val labels = reasons.map { getString(it.second) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quiz_report_dialog_title)
            .setItems(labels) { _, which ->
                showQuestionReportDetailDialog(reasons[which].first)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showQuestionReportDetailDialog(reasonCode: String) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.quiz_report_detail_hint)
            setHintTextColor(Color.parseColor("#8A96AD"))
            setTextColor(Color.WHITE)
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.quiz_report_detail_title)
            .setView(input)
            .setPositiveButton(R.string.quiz_report_submit) { _, _ ->
                submitQuestionReport(reasonCode, input.text?.toString())
            }
            .setNeutralButton(R.string.quiz_report_without_detail) { _, _ ->
                submitQuestionReport(reasonCode, null)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        disableOptions()
        markCorrectAnswer(state.question, state.userAnswer, state.isCorrect)
        
        showAnswerFeedback(state.isCorrect, state.explanation, state.userAnswer)
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
            binding.tvQuizQuestion.setTextColor(if (isCorrect) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        }

        // OX 유형
        if (question.type == QuestionType.OX) {
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
        // Chip 유형 (FILL, SITUATION, MULTIPLE)
        else {
            for (i in 0 until binding.layoutOptionsChips.childCount) {
                val chip = binding.layoutOptionsChips.getChildAt(i) as? Chip ?: continue
                if (chip.text == userAnswer) {
                    if (isCorrect) {
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

    private fun showAnswerFeedback(isCorrect: Boolean, explanation: String, userAnswer: String) {
        val question = getCurrentQuestion() ?: return
        markCorrectAnswer(question, userAnswer, isCorrect)

        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        
        if (!isCorrect) {
            lives--
            updateHearts(lives)
            shakeScreen()
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
                viewModel.finishQuizEarly()
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
            binding.tvFeedbackTitle.text = "${getString(R.string.quiz_feedback_wrong)} ${getString(R.string.quiz_feedback_wrong_hint)}"
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
            binding.layoutFeedbackPanel.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1025")))
        }
        binding.tvFeedbackContent.text = explanation
    }

    private fun showResult(result: QuizResult) {
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        // 펫 진화 축하 연출
        if (result.petEvolved) {
            showEvolutionCelebration()
        }

        binding.lavResultPet.setAnimation(R.raw.pet_idle)
        binding.lavResultPet.playAnimation()

        val isReviewSubmit = result.mode == "review"
        binding.tvResultStatus.text = when {
            isReviewSubmit && result.isPassed -> "복습 완료!"
            isReviewSubmit -> "복습 결과"
            result.isPassed -> getString(R.string.quiz_result_success)
            else -> getString(R.string.quiz_result_fail)
        }
        binding.tvResultStatus.setTextColor(
            if (result.isPassed) Color.parseColor("#00FFB2")
            else Color.parseColor("#FF4B4B")
        )

        binding.tvResultSub.text = when {
            isReviewSubmit && result.isPassed -> getString(R.string.quiz_result_review_subtitle_pass)
            isReviewSubmit -> getString(R.string.quiz_result_review_subtitle_fail)
            result.isPassed -> "정말 대단해! 리터러시 박사가 다 됐는걸?"
            else -> "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
        }
        
        binding.tvResCorrectCount.text = "${result.score} / ${result.total}"
        binding.tvResPassStatus.text = if (result.isPassed) "PASS" else "FAIL"
        binding.tvResPassStatus.setTextColor(
            if (result.isPassed) Color.parseColor("#00FFB2")
            else Color.parseColor("#FF4B4B")
        )

        // 오답 통계 표시
        if (!result.isPassed) {
            binding.layoutWrongStat.visibility = View.VISIBLE
            binding.tvResWrongCount.text = "${result.wrongCount}개"
            binding.layoutStatsContainer.weightSum = 4f
            binding.btnResRetry.visibility = View.VISIBLE
            binding.btnResFinish.text = "다음에 하기"
            binding.tvResPetBonus.setTextColor(Color.parseColor("#8A96AD"))
        } else {
            binding.layoutWrongStat.visibility = View.GONE
            binding.layoutStatsContainer.weightSum = 3f
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
            
            val question = state.quizQuestions.questions[index]
            
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
            val maxLives = if (viewModel.strictSingleLifeRetry.value) 1 else 3
            if (index == 0) updateHearts(maxLives, forceReset = true)
            updateQuizModeBanner()
            setupOptions(question)
            startTimer()
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
        val shakeCard = ObjectAnimator.ofFloat(binding.layoutQuestionCard, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        val shakeOptions = ObjectAnimator.ofFloat(binding.layoutOptionsContainer, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shakeCard.duration = 500
        shakeOptions.duration = 500
        shakeCard.start()
        shakeOptions.start()
    }

    private fun setupOptions(question: Question) {
        binding.layoutOptionsStandard.removeAllViews()
        binding.layoutOptionsChips.removeAllViews()
        
        // 초기화
        binding.layoutOptionsStandard.visibility = View.GONE
        binding.layoutOptionsOx.visibility = View.GONE
        binding.layoutOptionsChips.visibility = View.GONE

        when (question.type) {
            QuestionType.OX -> {
                binding.layoutOptionsOx.visibility = View.VISIBLE
                resetOxButtons()
            }
            QuestionType.FILL, QuestionType.SITUATION -> {
                binding.layoutOptionsChips.visibility = View.VISIBLE
                setupChips(question)
            }
            else -> {
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                question.options?.forEachIndexed { index, option ->
                    addOptionButton(binding.layoutOptionsStandard, option, index)
                }
            }
        }
    }

    private fun resetOxButtons() {
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70")))
        binding.btnOxO.setStrokeWidth((1 * resources.displayMetrics.density).toInt())
        binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70")))
        binding.btnOxX.setStrokeWidth((1 * resources.displayMetrics.density).toInt())
    }

    private fun setupChips(question: Question) {
        val density = resources.displayMetrics.density
        val isSituation = question.type == QuestionType.SITUATION
        
        if (isSituation) {
            binding.layoutOptionsChips.chipSpacingVertical = (12 * density).toInt()
        }

        question.options?.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option
                textSize = if (isSituation) 16f else 17f
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                isClickable = true
                isCheckable = false
                checkedIcon = null
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Color.WHITE)
                
                if (isSituation) {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (12 * density).toInt())
                    }
                    minHeight = (88 * density).toInt()
                    chipStartPadding = 24 * density
                    chipEndPadding = 24 * density
                    setChipBackgroundColorResource(android.R.color.transparent)
                    setBackgroundResource(R.drawable.bg_situation_card)
                    chipStrokeWidth = 0f
                } else {
                    minHeight = 56 * density.toInt()
                    chipStartPadding = 20 * density
                    chipEndPadding = 20 * density
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
                    
                    if (isSituation) {
                        setBackgroundResource(R.drawable.bg_situation_card_selected)
                    } else {
                        setChipBackgroundColorResource(R.color.quiz_mint)
                        setChipStrokeColorResource(R.color.quiz_mint)
                    }
                    setTextColor(Color.parseColor("#0A1633"))
                    
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (12 * density).toInt())
            }
            setBackgroundResource(R.drawable.home_card_bg_selector)
            setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
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
            textSize = 15f
        }

        val imageView = ImageView(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                (24 * density).toInt(),
                (24 * density).toInt()
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            setImageResource(R.drawable.ic_check_circle_mint)
            visibility = View.GONE
            tag = "check_icon"
        }

        relativeLayout.addView(textView)
        relativeLayout.addView(imageView)
        
        relativeLayout.setOnClickListener {
            imageView.visibility = View.VISIBLE
            handleOptionClick(text)
        }

        parent.addView(relativeLayout)
    }

    private fun handleOptionClick(answer: String) {
        timer?.cancel()
        val question = getCurrentQuestion() ?: return
        
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

        disableOptions()
        
        if (!viewModel.isSolutionMode.value) {
            viewModel.checkAnswer(question.id, answer)
        } else {
            viewModel.selectAnswer(question.id, answer)
        }
    }

    private fun getCurrentQuestion(): Question? {
        val state = viewModel.uiState.value
        return if (state is QuizUiState.QuestionLoaded) {
            state.quizQuestions.questions[viewModel.currentQuestionIndex.value]
        } else null
    }

    private fun disableOptions() {
        binding.btnOxO.isEnabled = false
        binding.btnOxX.isEnabled = false
        for (i in 0 until binding.layoutOptionsChips.childCount) {
            binding.layoutOptionsChips.getChildAt(i).isEnabled = false
        }
        for (i in 0 until binding.layoutOptionsStandard.childCount) {
            binding.layoutOptionsStandard.getChildAt(i).isEnabled = false
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
}
