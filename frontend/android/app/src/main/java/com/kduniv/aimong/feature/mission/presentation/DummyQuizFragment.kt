package com.kduniv.aimong.feature.mission.presentation

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentDummyQuizBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DummyQuizFragment : BaseFragment<FragmentDummyQuizBinding>(FragmentDummyQuizBinding::inflate) {

    data class DummyQuestion(
        val type: String,
        val typeLabel: String,
        val question: String,
        val options: List<String>,
        val correctAnswerIndex: Int,
        val feedback: String
    )

    private val questions = listOf(
        DummyQuestion("OX", "OX 퀴즈", "AI가 만든 글에는 항상 \"정확한 정보\"만 들어있다.", listOf("O", "X"), 1, "AI는 사실이 아닌 정보를 만들기도 해요. 이를 '환각'이라고 부른답니다."),
        DummyQuestion("MULTIPLE", "객관식", "모르는 사람이 내 \"학교 이름\"을 물어본다면?", listOf("알려준다", "부모님께 말씀드린다", "모른 척한다", "거짓말로 알려준다"), 1, "개인정보는 소중해요! 모르는 사람이 물어보면 반드시 어른에게 도움을 요청하세요."),
        DummyQuestion("FILL", "단어 채우기", "AI에게 명령어를 입력하는 것을 [      ]라고 불러요.", listOf("키워드", "패스워드", "프롬프트", "업데이트"), 2, "프롬프트(Prompt)는 AI와 대화하기 위한 입구와 같아요."),
        DummyQuestion("SITUATION", "상황 판단", "AI가 숙제를 대신 해줬을 때 가장 올바른 태도는?", listOf("그대로 제출", "내 생각인 척", "직접 다시 쓰기", "자랑하기"), 2, "AI는 도구일 뿐이에요. 내 생각을 키우는 과정이 꼭 필요해요!"),
        DummyQuestion("OX", "OX 퀴즈", "AI는 사람처럼 \"감정\"을 느낄 수 있다.", listOf("O", "X"), 1, "AI는 계산된 결과로 답변할 뿐, 사람처럼 감정을 느끼지는 못해요."),
        DummyQuestion("MULTIPLE", "객관식", "AI를 올바르게 사용하는 태도가 \"아닌\" 것은?", listOf("비판적 생각", "예의 바른 대화", "무조건 믿기", "출처 확인"), 2, "AI도 틀릴 수 있다는 점을 기억하고 비판적으로 생각해야 해요."),
        DummyQuestion("FILL", "단어 채우기", "AI 답변이 진짜인지 가짜인지 확인하는 과정을 [      ]라고 해요.", listOf("팩트체크", "로그인", "다운로드", "댓글달기"), 0, "인터넷 정보는 항상 의심해보고 확인하는 습관이 중요해요."),
        DummyQuestion("SITUATION", "상황 판단", "AI가 기분 나쁜 말을 했을 때 어떻게 해야 할까요?", listOf("똑같이 욕한다", "무시한다", "당장 대화를 멈춘다", "컴퓨터를 끈다"), 2, "부적절한 대화는 언제든지 멈추고 어른에게 알려야 해요."),
        DummyQuestion("OX", "OX 퀴즈", "개인정보는 \"나만\" 알고 있어야 한다.", listOf("O", "X"), 0, "맞아요! 소중한 내 정보는 스스로가 먼저 지켜야 한답니다."),
        DummyQuestion("MULTIPLE", "객관식", "다음 중 AI 기술이 들어간 물건이 \"아닌\" 것은?", listOf("챗봇", "스마트 냉장고", "일반 종이 책", "자율주행 자동차"), 2, "종이 책은 사람이 직접 읽고 쓰는 전통적인 매체예요.")
    )

    private var currentQuestionIndex = 0
    private var lastAttemptedIndex = 0
    private val totalQuestions = 10
    private var lives = 3
    private var correctCount = 0
    private var isSolutionMode = false
    private val userAnswers = mutableMapOf<Int, Int>()
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun initView() {
        resetQuizState()
        setupClickListeners()
        setupQuizUI()
    }

    private fun resetQuizState() {
        currentQuestionIndex = 0
        lastAttemptedIndex = 0
        lives = 3
        correctCount = 0
        isSolutionMode = false
        userAnswers.clear()
        binding.layoutQuizResult.visibility = View.GONE
        binding.layoutHearts.visibility = View.VISIBLE
        binding.tvExpInfo.visibility = View.GONE
        binding.tvExpInfo.text = ""
        updateHearts(animate = false)
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnDummyOpt1.setOnClickListener { handleOptionClick(0) }
        binding.btnDummyOpt2.setOnClickListener { handleOptionClick(1) }
        binding.btnDummyOpt3.setOnClickListener { handleOptionClick(2) }
        binding.btnDummyOpt4.setOnClickListener { handleOptionClick(3) }
        binding.btnOxO.setOnClickListener { handleOptionClick(0) }
        binding.btnOxX.setOnClickListener { handleOptionClick(1) }
        
        binding.btnNextQuestion.setOnClickListener {
            if (isSolutionMode) {
                if (currentQuestionIndex < lastAttemptedIndex) {
                    currentQuestionIndex++
                    setupQuizUI()
                } else {
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    binding.layoutQuizResult.visibility = View.VISIBLE
                }
            } else {
                if (lives <= 0) {
                    showResultOverlay(isSuccess = false)
                } else if (currentQuestionIndex < totalQuestions - 1) {
                    currentQuestionIndex++
                    resetOptions()
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    setupQuizUI()
                } else {
                    showResultOverlay(isSuccess = true)
                }
            }
        }

        binding.btnFeedbackRetry.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            resetOptions()
        }

        // 결과 화면 버튼
        binding.btnResRetry.setOnClickListener { initView() }
        binding.btnResFinish.setOnClickListener { findNavController().popBackStack() }
        binding.btnResViewSolutions.setOnClickListener {
            isSolutionMode = true
            currentQuestionIndex = 0
            binding.layoutQuizResult.visibility = View.GONE
            setupQuizUI()
        }
    }

    private fun setupQuizUI() {
        val q = questions[currentQuestionIndex]
        binding.tvQuestionCount.text = getString(R.string.quiz_count_format, currentQuestionIndex + 1, totalQuestions)
        binding.pbQuizProgress.progress = currentQuestionIndex + 1
        
        if (!isSolutionMode) {
            startTimer(30000)
        }
        
        var displayQuestion = q.question
        val userAnswer = userAnswers[currentQuestionIndex] ?: -1
        
        // 풀이 모드에서는 정답을, 일반 모드에서는 선택한 답변이 있으면 빈칸에 채움
        val answerIndexToShow = if (isSolutionMode) q.correctAnswerIndex else userAnswer
        
        if (q.type == "FILL" && answerIndexToShow != -1) {
            displayQuestion = q.question.replace("[      ]", "[${q.options[answerIndexToShow]}]")
        }

        setHighlightedText(binding.tvQuizQuestion, displayQuestion)
        
        when(q.type) {
            "OX" -> {
                binding.layoutOptionsStandard.visibility = View.GONE
                binding.layoutOptionsOx.visibility = View.VISIBLE
                binding.layoutOptionsChips.visibility = View.GONE
            }
            "FILL" -> {
                binding.layoutOptionsStandard.visibility = View.GONE
                binding.layoutOptionsOx.visibility = View.GONE
                binding.layoutOptionsChips.visibility = View.VISIBLE
                setupChips(q.options)
            }
            else -> { // MULTIPLE, SITUATION
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                binding.layoutOptionsOx.visibility = View.GONE
                binding.layoutOptionsChips.visibility = View.GONE
                
                val buttons = listOf(binding.btnDummyOpt1, binding.btnDummyOpt2, binding.btnDummyOpt3, binding.btnDummyOpt4)
                val texts = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
                val checks = listOf(binding.ivCheck1, binding.ivCheck2, binding.ivCheck3, binding.ivCheck4)

                buttons.forEachIndexed { i, btn ->
                    if (i < q.options.size) {
                        btn.visibility = View.VISIBLE
                        texts[i].text = if (q.type == "SITUATION") q.options[i] else "${i + 1}. ${q.options[i]}"
                        texts[i].isSingleLine = false
                        checks[i].visibility = View.GONE
                    } else {
                        btn.visibility = View.GONE
                    }
                }
            }
        }
        
        if (isSolutionMode) showSolutionForCurrentQuestion() else resetOptions()
    }

    private fun handleOptionClick(optionIndex: Int) {
        if (isSolutionMode) return
        countDownTimer?.cancel()

        val q = questions[currentQuestionIndex]
        val isCorrect = if (optionIndex == -1) false else optionIndex == q.correctAnswerIndex
        userAnswers[currentQuestionIndex] = optionIndex

        disableOptions()
        if (optionIndex != -1) {
            highlightSelectedOption(optionIndex, q.type)
            if (q.type == "FILL") {
                val filledQuestion = q.question.replace("[      ]", "[${q.options[optionIndex]}]")
                setHighlightedText(binding.tvQuizQuestion, filledQuestion)
            }
        }

        view?.postDelayed({
            if (!isAdded) return@postDelayed
            
            if (!isCorrect) {
                lives--
                updateHearts(animate = true)
                shakeView(binding.layoutHearts)
                
                if (lives <= 0) {
                    showMissionFailure()
                    return@postDelayed
                }
            }

            if (currentQuestionIndex < totalQuestions - 1) {
                currentQuestionIndex++
                resetOptions()
                setupQuizUI()
            } else {
                showResultOverlay(isSuccess = true)
            }
        }, 600)
    }

    private fun highlightSelectedOption(index: Int, type: String) {
        when(type) {
            "OX" -> {
                val btn = if (index == 0) binding.btnOxO else binding.btnOxX
                val color = if (index == 0) "#00FFB2" else "#FF4B4B"
                btn.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor(color)))
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor(color)))
                
                if (index == 0) {
                    binding.tvOxOIcon.setTextColor(Color.parseColor("#0A1633"))
                    binding.tvOxOText.setTextColor(Color.parseColor("#0A1633"))
                } else {
                    binding.tvOxXIcon.setTextColor(Color.parseColor("#0A1633"))
                    binding.tvOxXText.setTextColor(Color.parseColor("#0A1633"))
                }
            }
            "FILL" -> {
                val card = binding.layoutOptionsChips.getChildAt(index) as? com.google.android.material.card.MaterialCardView
                card?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                card?.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                (card?.getChildAt(0) as? TextView)?.setTextColor(Color.parseColor("#0A1633"))
            }
            else -> {
                val buttons = listOf(binding.btnDummyOpt1, binding.btnDummyOpt2, binding.btnDummyOpt3, binding.btnDummyOpt4)
                if (index in buttons.indices) {
                    buttons[index].isSelected = true
                    buttons[index].setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                }
            }
        }
    }

    private fun showResultOverlay(isSuccess: Boolean) {
        countDownTimer?.cancel()
        lastAttemptedIndex = currentQuestionIndex

        // 결과 산출
        correctCount = 0
        questions.forEachIndexed { index, q ->
            if (userAnswers[index] == q.correctAnswerIndex) {
                correctCount++
            }
        }

        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        val isPassed = isSuccess && correctCount >= 8
        binding.tvResultStatus.text = if (isPassed) "미션 성공!" else "조금 더 노력해봐!"
        binding.tvResultStatus.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        
        binding.tvResultSub.text = if (isPassed) "정말 대단해! 리터러시 박사가 다 됐는걸?" else "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
        
        binding.tvResCorrectCount.text = "$correctCount / $totalQuestions"
        binding.tvResPassStatus.text = if (isPassed) "PASS" else "FAIL"
        binding.tvResPassStatus.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))

        if (!isPassed) {
            binding.layoutWrongStat.visibility = View.VISIBLE
            binding.tvResWrongCount.text = "${totalQuestions - correctCount}개"
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
        
        val gainedXp = if (isPassed) (if (correctCount == 10) 70 else 50) else 0
        animateXpGain(gainedXp)
        
        binding.tvResPetBonus.text = if (isPassed) "+15% XP" else "+0% XP"
        binding.layoutRewardsRow.visibility = if (isPassed) View.VISIBLE else View.GONE
        
        binding.lavResultPet.setAnimation(R.raw.pet_idle)
        binding.lavResultPet.playAnimation()
    }

    private fun animateXpGain(gainedXp: Int) {
        val startXp = 850
        val maxXp = 1000
        val endXp = (startXp + gainedXp).coerceAtMost(maxXp)
        
        binding.tvResXpGain.text = "+$gainedXp XP"
        
        ValueAnimator.ofInt(startXp, endXp).apply {
            duration = 1500
            addUpdateListener { 
                val value = it.animatedValue as Int
                binding.pbResXpProgress.progress = value
                binding.tvResXpStatus.text = "LV.5 ($value / $maxXp)"
            }
            start()
        }
    }

    private fun startTimer(millis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : android.os.CountDownTimer(millis, 1000) {
            override fun onTick(t: Long) {
                val sec = t / 1000
                binding.tvTimer.text = String.format("⏱ %02d:%02d 남음", sec / 60, sec % 60)
                binding.tvTimer.setTextColor(if (t <= 10000) Color.RED else Color.parseColor("#8A96AD"))
            }
            override fun onFinish() { handleOptionClick(-1) }
        }.start()
    }

    private fun setupChips(options: List<String>) {
        binding.layoutOptionsChips.removeAllViews()
        val density = resources.displayMetrics.density
        options.forEachIndexed { index, opt ->
            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = 16 * density
                strokeWidth = (1 * density).toInt()
                setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70")))
                setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A2B52")))
                
                val tv = TextView(context).apply {
                    text = opt
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                    setPadding((20 * density).toInt(), (12 * density).toInt(), (20 * density).toInt(), (12 * density).toInt())
                }
                addView(tv)
                setOnClickListener { handleOptionClick(index) }
            }
            binding.layoutOptionsChips.addView(card)
        }
    }

    private fun setHighlightedText(view: TextView, text: String) {
        val spannable = SpannableString(text)
        val highlightColor = Color.parseColor("#00FFB2")
        val quoteColor = Color.parseColor("#FFD600")

        // Highlight [ ... ]
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

        // Highlight text inside double quotes
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

    private fun updateHearts(animate: Boolean) {
        val hearts = listOf(binding.ivHeart1, binding.ivHeart2, binding.ivHeart3)
        hearts.forEachIndexed { i, iv ->
            iv.setImageResource(if (i < lives) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty)
            if (animate && i == lives) animateHeartLoss(iv)
        }
    }

    private fun animateHeartLoss(view: ImageView) {
        ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("scaleX", 1.2f, 0f), PropertyValuesHolder.ofFloat("scaleY", 1.2f, 0f)).apply {
            duration = 400
            start()
        }
    }

    private fun shakeView(v: View) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, 10f).apply { duration = 500; interpolator = CycleInterpolator(3f); start() }
    }

    private fun showMissionFailure() {
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = "❌ 미션 실패..."
        binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
        binding.tvFeedbackContent.text = "오답을 3번이나 했어요. 다시 도전해볼까요?"
        binding.btnNextQuestion.text = "결과 확인하기"
        binding.btnFeedbackRetry.visibility = View.GONE
    }

    private fun resetOptions() {
        val defaultStroke = Color.parseColor("#243B70")
        val defaultBg = Color.parseColor("#1A2B52")

        listOf(binding.btnDummyOpt1, binding.btnDummyOpt2, binding.btnDummyOpt3, binding.btnDummyOpt4, binding.btnOxO, binding.btnOxX).forEach {
            it.isEnabled = true; it.isSelected = false
            (it as? com.google.android.material.card.MaterialCardView)?.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultStroke))
            (it as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(defaultBg))
        }

        binding.tvOxOIcon.setTextColor(Color.parseColor("#00FFB2"))
        binding.tvOxOText.setTextColor(Color.parseColor("#00FFB2"))
        binding.tvOxXIcon.setTextColor(Color.parseColor("#FF4B4B"))
        binding.tvOxXText.setTextColor(Color.parseColor("#FF4B4B"))

        for (i in 0 until binding.layoutOptionsChips.childCount) {
            val card = binding.layoutOptionsChips.getChildAt(i) as? com.google.android.material.card.MaterialCardView
            card?.isEnabled = true
            card?.setStrokeColor(android.content.res.ColorStateList.valueOf(defaultStroke))
            card?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(defaultBg))
            (card?.getChildAt(0) as? TextView)?.setTextColor(Color.WHITE)
        }

        listOf(binding.ivCheck1, binding.ivCheck2, binding.ivCheck3, binding.ivCheck4).forEach { it.visibility = View.GONE }
        binding.btnNextQuestion.text = "다음 문제 →"
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.btnFeedbackRetry.visibility = View.VISIBLE
    }

    private fun disableOptions() {
        listOf(binding.btnDummyOpt1, binding.btnDummyOpt2, binding.btnDummyOpt3, binding.btnDummyOpt4, binding.btnOxO, binding.btnOxX).forEach { it.isEnabled = false }
        for (i in 0 until binding.layoutOptionsChips.childCount) {
            binding.layoutOptionsChips.getChildAt(i).isEnabled = false
        }
    }

    private fun showSolutionForCurrentQuestion() {
        val q = questions[currentQuestionIndex]
        val userIdx = userAnswers[currentQuestionIndex] ?: -1
        val correctIdx = q.correctAnswerIndex
        
        disableOptions()
        
        // 정답 및 오답 하이라이트
        if (q.type == "OX") {
            if (userIdx == 0) binding.btnOxO.isSelected = true
            if (userIdx == 1) binding.btnOxX.isSelected = true
            
            val correctBtn = if (correctIdx == 0) binding.btnOxO else binding.btnOxX
            correctBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
            
            if (userIdx != -1 && userIdx != correctIdx) {
                val wrongBtn = if (userIdx == 0) binding.btnOxO else binding.btnOxX
                wrongBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
            }
        } else if (q.type == "FILL") {
            for (i in 0 until binding.layoutOptionsChips.childCount) {
                val card = binding.layoutOptionsChips.getChildAt(i) as? com.google.android.material.card.MaterialCardView
                val tv = card?.getChildAt(0) as? TextView
                if (i == correctIdx) {
                    card?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                    card?.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                    tv?.setTextColor(Color.parseColor("#0A1633"))
                } else if (i == userIdx) {
                    card?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                    card?.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                }
            }
        } else {
            val buttons = listOf(binding.btnDummyOpt1, binding.btnDummyOpt2, binding.btnDummyOpt3, binding.btnDummyOpt4)
            val checks = listOf(binding.ivCheck1, binding.ivCheck2, binding.ivCheck3, binding.ivCheck4)
            buttons.forEachIndexed { i, btn ->
                if (i == correctIdx) {
                    btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                    checks[i].visibility = View.VISIBLE
                    checks[i].setImageResource(R.drawable.ic_check_circle)
                    checks[i].setColorFilter(Color.parseColor("#00FFB2"))
                } else if (i == userIdx) {
                    btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                }
            }
        }
        
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = if (userIdx == correctIdx) "🎉 정답이에요!" else "😥 아쉬워요!"
        binding.tvFeedbackTitle.setTextColor(if (userIdx == correctIdx) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        binding.tvFeedbackContent.text = q.feedback

        binding.btnFeedbackRetry.visibility = View.GONE
        binding.btnNextQuestion.text = if (currentQuestionIndex == lastAttemptedIndex) "결과로 돌아가기" else "다음 풀이 →"
    }

    override fun initObserver() {}
}
