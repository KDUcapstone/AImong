package com.kduniv.aimong.feature.quiz.presentation

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private val viewModel: QuizViewModel by viewModels()

    private var lives = 3

    override fun initView() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnViewSolutions.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            viewModel.startSolutionMode()
        }
        binding.btnRetryQuiz.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            viewModel.retryQuiz()
        }
        binding.btnFinish.setOnClickListener {
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
                    }
                }
                launch {
                    viewModel.timeLeft.collect { millis ->
                        val seconds = millis / 1000
                        val minutes = seconds / 60
                        val remainingSeconds = seconds % 60
                        binding.tvTimer.text = String.format(java.util.Locale.getDefault(), "⏱ %02d:%02d 남음", minutes, remainingSeconds)

                        if (millis in 1..10000) {
                            binding.tvTimer.setTextColor(Color.RED)
                        } else {
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
                updateQuestion(viewModel.currentQuestionIndex.value)
            }
            is QuizUiState.AnswerChecked -> {
                showAnswerFeedback(state.isCorrect, state.explanation)
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
        
        showAnswerFeedback(state.isCorrect, state.explanation)
        binding.btnFeedbackRetry.visibility = View.GONE // 풀이 모드에선 다시보기 불필요
        binding.btnNextQuestion.text = if (index == total - 1) "결과로 돌아가기" else "다음 풀이 →"
    }

    private fun markCorrectAnswer(question: Question, userAnswer: String, isCorrect: Boolean) {
        val density = resources.displayMetrics.density
        
        // FILL 유형: 질문 텍스트의 빈칸을 사용자 답변으로 교체 (v2.3 명세 준수)
        if (question.type == QuestionType.FILL) {
            val originalText = question.question
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $userAnswer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $userAnswer ")
                else -> "$originalText\n\n정답 확인: $userAnswer"
            }
            binding.tvQuizQuestion.text = replacedText
            binding.tvQuizQuestion.setTextColor(if (isCorrect) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        }

        // OX 유형
        if (question.type == QuestionType.OX) {
            val correctLabel = if (isCorrect) userAnswer else (if (userAnswer == "O") "X" else "O")
            if (correctLabel == "O") {
                binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                binding.btnOxO.setStrokeWidth((4 * density).toInt())
            } else {
                binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                binding.btnOxX.setStrokeWidth((4 * density).toInt())
            }
            
            if (!isCorrect) {
                if (userAnswer == "O") {
                    binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                } else {
                    binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
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

    private fun showAnswerFeedback(isCorrect: Boolean, explanation: String) {
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        if (isCorrect) {
            binding.tvFeedbackTitle.text = getString(R.string.quiz_feedback_correct)
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#00FFB2"))
        } else {
            binding.tvFeedbackTitle.text = getString(R.string.quiz_feedback_wrong)
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
        }
        binding.tvFeedbackContent.text = explanation
    }

    private fun showResult(result: QuizResult) {
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        // 펫 진화 축하 연출 (petEvolved 플래그 확인)
        if (result.petEvolved) {
            showEvolutionCelebration()
        }

        val animFile = if (result.isPassed) "pet_happy.json" else "pet_sad.json"
        // TODO: 실제 pet_happy.json, pet_sad.json 파일이 추가되어야 함. 현재는 pet_idle.json으로 대체하거나 로직만 유지
        // com.kduniv.aimong.core.util.LottieUtils.playAnimation(binding.lavFeedback, animFile)

        // 결과 하트 표시
        val finalLives = when {
            result.score >= 9 -> 3
            result.score >= 7 -> 2
            result.score >= 5 -> 1
            else -> 0
        }
        val heartViews = listOf(binding.ivResultHeart1, binding.ivResultHeart2, binding.ivResultHeart3)
        heartViews.forEachIndexed { index, imageView ->
            imageView.setImageResource(R.drawable.ic_heart_empty)
            if (index < finalLives) {
                imageView.postDelayed({
                    imageView.setImageResource(R.drawable.ic_heart_filled)
                    imageView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                        imageView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                }, index * 300L)
            }
        }

        binding.tvResultTitle.text = if (result.isPassed) getString(R.string.quiz_result_success) else getString(R.string.quiz_result_fail)
        binding.tvResultTitle.setTextColor(
            if (result.isPassed) ContextCompat.getColor(requireContext(), R.color.quiz_mint)
            else ContextCompat.getColor(requireContext(), R.color.quiz_red)
        )
        
        binding.tvResultScore.text = getString(R.string.quiz_result_score, result.total, result.score)
        
        // XP 및 보너스 정보 표시 (v2.3 보너스 정책 반영)
        val xpText = StringBuilder("+$result.xpEarned EXP 획득")
        val bonuses = mutableListOf<String>()
        if (result.isPerfect) bonuses.add("퍼펙트 +10")
        if (result.bonusXp > 0) {
            val reason = getBonusReasonText(result.bonusReason)
            bonuses.add("$reason +$result.bonusXp")
        }
        
        if (bonuses.isNotEmpty()) {
            xpText.append(" (${bonuses.joinToString(", ")})")
        }
        
        binding.tvResultXp.text = xpText.toString()
        binding.tvResultXp.visibility = if (result.xpEarned > 0) View.VISIBLE else View.GONE
        
        // 스트릭 정보
        if (result.streakDays > 0) {
            binding.tvStreakInfo.visibility = View.VISIBLE
            binding.tvStreakInfo.text = "🔥 ${result.streakDays}일 연속 스트릭 유지 중!"
        } else {
            binding.tvStreakInfo.visibility = View.GONE
        }

        // 보상 아이템(티켓 등) 표시
        binding.layoutRewardsContainer.removeAllViews()
        result.rewards.forEach { reward ->
            addRewardIcon(reward)
        }
        
        binding.tvWrongCount.text = "오답: ${result.total - result.score}개"
        
        binding.btnViewSolutions.setOnClickListener {
            viewModel.startSolutionMode()
            binding.layoutQuizResult.visibility = View.GONE
        }
        
        binding.btnRetryQuiz.setOnClickListener {
            viewModel.retryQuiz()
            binding.layoutQuizResult.visibility = View.GONE
        }

        binding.btnFinish.setOnClickListener {
            requireActivity().finish()
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
            binding.tvQuizQuestion.text = question.question
            binding.tvQuizQuestion.setTextColor(Color.WHITE) // 색상 초기화
            
            updateHearts(3) // TODO: 실제 라이프 데이터 연동 필요 시 수정
            setupOptions(question)
        }
    }

    private fun updateHearts(newLives: Int) {
        val hearts = listOf(binding.ivHeart1, binding.ivHeart2, binding.ivHeart3)
        
        if (newLives < lives) {
            // 하트 소실 애니메이션 (마지막 하트)
            val indexToAnimate = lives - 1
            if (indexToAnimate in 0..2) {
                val heartView = hearts[indexToAnimate]
                heartView.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        heartView.setImageResource(R.drawable.ic_heart_empty)
                        heartView.scaleX = 1f
                        heartView.scaleY = 1f
                        heartView.alpha = 1f
                    }
                    .start()
            }
        } else {
            // 초기화 또는 복구
            hearts.forEachIndexed { index, imageView ->
                val isFilled = index < newLives
                val targetRes = if (isFilled) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
                imageView.setImageResource(targetRes)
            }
        }
        lives = newLives
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
        val question = getCurrentQuestion() ?: return
        
        // FILL 유형 시각적 피드백: 빈칸 채우기 (v2.3 명세 준수 - 즉시 치환)
        if (question.type == QuestionType.FILL) {
            val originalText = question.question
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $answer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $answer ")
                else -> originalText
            }
            binding.tvQuizQuestion.text = replacedText
            binding.tvQuizQuestion.setTextColor(Color.WHITE)
        }

        disableOptions()
        viewModel.selectAnswer(question.id, answer)
        
        // v1.3 준수: 퀴즈 도중에는 피드백 없이 0.5초 후 다음 문제로 자동 이동
        if (!viewModel.isSolutionMode.value) {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(500)
                viewModel.nextQuestion()
            }
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
