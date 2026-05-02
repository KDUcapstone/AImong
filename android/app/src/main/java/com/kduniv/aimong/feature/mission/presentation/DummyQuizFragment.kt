package com.kduniv.aimong.feature.mission.presentation

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.CycleInterpolator
import android.widget.ImageView
import android.widget.TextView
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
        DummyQuestion(
            "OX",
            "OX 퀴즈",
            "AI가 만든 글에는 항상 \"정확한 정보\"만 들어있다.",
            listOf("O", "X"),
            1,
            "AI는 그럴듯하지만 사실이 아닌 정보를 만들기도 해요. 이를 '환각(Hallucination)'이라고 부른답니다."
        ),
        DummyQuestion(
            "MULTIPLE",
            "객관식",
            "인터넷에서 모르는 사람이 내 \"학교 이름\"을 물어본다면 어떻게 해야 할까요?",
            listOf("알려준다", "부모님께 말씀드린다", "모른 척한다", "거짓말로 알려준다"),
            1,
            "개인정보는 소중해요! 모르는 사람이 물어보면 반드시 어른에게 도움을 요청하세요."
        ),
        DummyQuestion(
            "FILL",
            "단어 채우기",
            "AI에게 명령어를 입력하는 것을 [      ]라고 불러요. (힌트: ㅍㄹㅍㅌ)",
            listOf("키워드", "패스워드", "프롬프트", "업데이트"),
            2,
            "프롬프트(Prompt)는 AI와 대화하기 위한 입구와 같아요. 정확하게 입력할수록 좋은 답변이 온답니다."
        ),
        DummyQuestion(
            "SITUATION",
            "상황 판단",
            "상황: AI가 숙제를 대신 해줬어요. 이때 가장 올바른 태도는 무엇일까요?",
            listOf("그대로 제출한다", "내 생각인 척한다", "참고만 하고 직접 다시 쓴다", "친구들에게 자랑한다"),
            2,
            "AI는 도구일 뿐이에요. 내 생각을 키우기 위해서는 직접 고민하는 과정이 꼭 필요해요!"
        ),
        DummyQuestion(
            "OX",
            "OX 퀴즈",
            "AI는 사람처럼 \"감정\"을 느낄 수 있다.",
            listOf("O", "X"),
            1,
            "AI는 계산된 결과로 답변할 뿐, 사람처럼 감정을 느끼지는 못해요."
        ),
        DummyQuestion(
            "MULTIPLE",
            "객관식",
            "다음 중 AI를 올바르게 사용하는 태도가 \"아닌\" 것은?",
            listOf("비판적으로 생각하기", "예의 바르게 대화하기", "무조건 믿기", "출처 확인하기"),
            2,
            "AI도 틀릴 수 있다는 점을 항상 기억해야 해요. 비판적인 사고가 중요합니다."
        ),
        DummyQuestion(
            "FILL",
            "단어 채우기",
            "AI 답변이 진짜인지 가짜인지 확인하는 과정을 [      ]라고 해요. (힌트: ㅍㅌㅊㅋ)",
            listOf("팩트체크", "로그인", "다운로드", "댓글달기"),
            0,
            "인터넷 정보는 항상 의심해보고 확인하는 습관(팩트체크)이 중요해요."
        ),
        DummyQuestion(
            "SITUATION",
            "상황 판단",
            "상황: AI가 기분 나쁜 말을 했어요. 이때 어떻게 해야 할까요?",
            listOf("똑같이 욕한다", "무시하고 계속한다", "당장 대화를 멈춘다", "컴퓨터를 끈다"),
            2,
            "기분이 나쁘거나 부적절한 대화는 언제든지 멈추고 어른에게 알려야 해요."
        ),
        DummyQuestion(
            "OX",
            "OX 퀴즈",
            "개인정보는 \"나만\" 알고 있어야 한다.",
            listOf("O", "X"),
            0,
            "맞아요! 소중한 내 정보는 스스로가 가장 먼저 지켜야 한답니다."
        ),
        DummyQuestion(
            "MULTIPLE",
            "객관식",
            "다음 중 AI 기술이 들어간 물건이 \"아닌\" 것은?",
            listOf("챗봇", "스마트 냉장고", "일반 종이 책", "자율주행 자동차"),
            2,
            "종이 책은 사람이 직접 읽고 쓰는 전통적인 매체예요. 기술이 들어가면 '전자책'이 되죠."
        )
    )

    private var currentQuestionIndex = 0
    private val totalQuestions = 10
    private var lives = 3
    private var wrongCount = 0
    private var correctCount = 0
    private var isSolutionMode = false
    private val userAnswers = mutableMapOf<Int, Int>()

    override fun initView() {
        currentQuestionIndex = 0
        lives = 3
        wrongCount = 0
        correctCount = 0
        isSolutionMode = false
        userAnswers.clear()

        setupQuizUI()
        
        binding.btnDummyOpt1.setOnClickListener { handleOptionClick(0) }
        binding.btnDummyOpt2.setOnClickListener { handleOptionClick(1) }
        binding.btnDummyOpt3.setOnClickListener { handleOptionClick(2) }
        binding.btnDummyOpt4.setOnClickListener { handleOptionClick(3) }
        
        binding.btnOxO.setOnClickListener { handleOptionClick(0) }
        binding.btnOxX.setOnClickListener { handleOptionClick(1) }
        
        binding.btnNextQuestion.setOnClickListener {
            if (isSolutionMode) {
                if (currentQuestionIndex < totalQuestions - 1) {
                    currentQuestionIndex++
                    setupQuizUI()
                } else {
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    binding.layoutQuizResult.visibility = View.VISIBLE
                }
            } else {
                if (currentQuestionIndex < totalQuestions - 1) {
                    currentQuestionIndex++
                    resetOptions()
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    setupQuizUI()
                } else {
                    showResultOverlay(isSuccess = true)
                }
            }
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.btnFeedbackRetry.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            resetOptions()
        }

        // 결과 오버레이 버튼들
        binding.btnViewSolutions.setOnClickListener {
            isSolutionMode = true
            currentQuestionIndex = 0
            binding.layoutQuizResult.visibility = View.GONE
            setupQuizUI()
        }
        binding.btnRetryQuiz.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            initView() // 처음부터 다시
        }
        binding.btnFinish.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupQuizUI() {
        val q = questions[currentQuestionIndex]
        binding.tvQuestionCount.text = "${currentQuestionIndex + 1} / $totalQuestions 문제"
        binding.pbQuizProgress.progress = currentQuestionIndex + 1
        
        if (!isSolutionMode) {
            updateHearts(animate = false)
        }
        
        // 유형별 텍스트 조합 및 하이라이트
        val fullText = "[${q.typeLabel}] ${q.question}"
        setHighlightedText(binding.tvQuizQuestion, fullText, q.typeLabel)
        binding.tvQuizQuestion.setTextColor(Color.WHITE)
        
        // 유형별 옵션 가시성 조절
        when(q.type) {
            "OX" -> {
                binding.layoutOptionsStandard.visibility = View.GONE
                binding.layoutOptionsOx.visibility = View.VISIBLE
                binding.layoutOptionsChips.visibility = View.GONE
                if (isSolutionMode) {
                    showSolutionForCurrentQuestion()
                } else {
                    resetOptions()
                }
            }
            "FILL", "SITUATION" -> {
                binding.layoutOptionsStandard.visibility = View.GONE
                binding.layoutOptionsOx.visibility = View.GONE
                binding.layoutOptionsChips.visibility = View.VISIBLE
                setupChips(q.options)
                if (isSolutionMode) {
                    showSolutionForCurrentQuestion()
                }
            }
            else -> {
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                binding.layoutOptionsOx.visibility = View.GONE
                binding.layoutOptionsChips.visibility = View.GONE
                binding.btnDummyOpt1.visibility = View.VISIBLE
                binding.btnDummyOpt2.visibility = View.VISIBLE
                binding.btnDummyOpt3.visibility = View.VISIBLE
                binding.btnDummyOpt4.visibility = View.VISIBLE
                binding.tvOpt1.text = "①  " + q.options[0]
                binding.tvOpt2.text = "②  " + q.options[1]
                binding.tvOpt3.text = "③  " + q.options[2]
                binding.tvOpt4.text = "④  " + q.options[3]
                if (isSolutionMode) {
                    showSolutionForCurrentQuestion()
                } else {
                    resetOptions()
                }
            }
        }
        
        if (!isSolutionMode) {
            binding.layoutFeedbackPanel.visibility = View.GONE
        }
    }

    private fun showSolutionForCurrentQuestion() {
        val q = questions[currentQuestionIndex]
        val userAnswerIndex = userAnswers[currentQuestionIndex] ?: -1
        val isCorrect = userAnswerIndex == q.correctAnswerIndex
        
        disableOptions()
        
        // FILL 유형 텍스트 치환
        if (q.type == "FILL" && userAnswerIndex != -1) {
            val selectedAnswer = q.options[userAnswerIndex]
            val originalText = "[${q.typeLabel}] ${q.question}"
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $selectedAnswer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $selectedAnswer ")
                else -> originalText
            }
            binding.tvQuizQuestion.text = replacedText
            binding.tvQuizQuestion.setTextColor(if (isCorrect) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        }

        // 옵션 하이라이트
        when (q.type) {
            "OX" -> {
                if (userAnswerIndex == 0) binding.btnOxO.isSelected = true
                if (userAnswerIndex == 1) binding.btnOxX.isSelected = true
                
                // 정답 표시 (민트 테두리)
                if (q.correctAnswerIndex == 0) {
                    binding.btnOxO.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                    binding.btnOxO.setStrokeWidth(8)
                } else {
                    binding.btnOxX.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                    binding.btnOxX.setStrokeWidth(8)
                }
                
                // 오답일 경우 사용자 선택 레드 표시
                if (!isCorrect && userAnswerIndex != -1) {
                    val wrongView = if (userAnswerIndex == 0) binding.btnOxO else binding.btnOxX
                    wrongView.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                    wrongView.setStrokeWidth(8)
                }
            }
            "FILL", "SITUATION" -> {
                for (i in 0 until binding.layoutOptionsChips.childCount) {
                    val chip = binding.layoutOptionsChips.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                    if (i == q.correctAnswerIndex) {
                        chip.setChipBackgroundColorResource(R.color.quiz_mint)
                        chip.setTextColor(Color.parseColor("#0A1633"))
                    } else if (i == userAnswerIndex) {
                        chip.setChipBackgroundColorResource(R.color.quiz_red)
                        chip.setTextColor(Color.WHITE)
                    }
                }
            }
            else -> {
                val correctView = when(q.correctAnswerIndex) {
                    0 -> binding.btnDummyOpt1
                    1 -> binding.btnDummyOpt2
                    2 -> binding.btnDummyOpt3
                    else -> binding.btnDummyOpt4
                }
                correctView.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FFB2")))
                correctView.setStrokeWidth(4)
                
                if (!isCorrect && userAnswerIndex != -1) {
                    val userView = when(userAnswerIndex) {
                        0 -> binding.btnDummyOpt1
                        1 -> binding.btnDummyOpt2
                        2 -> binding.btnDummyOpt3
                        else -> binding.btnDummyOpt4
                    }
                    userView.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF4B4B")))
                    userView.setStrokeWidth(4)
                }
            }
        }
        
        showFeedback(isCorrect)
        binding.btnFeedbackRetry.visibility = View.GONE
        binding.btnNextQuestion.text = if (currentQuestionIndex == totalQuestions - 1) "결과로 돌아가기" else "다음 풀이 →"
    }

    private fun setupChips(options: List<String>) {
        binding.layoutOptionsChips.removeAllViews()
        val density = resources.displayMetrics.density
        options.forEachIndexed { index, option ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = option
                textSize = 17f
                typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
                isClickable = true
                isCheckable = true
                isCheckedIconVisible = false
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Color.WHITE)
                setChipBackgroundColorResource(R.color.home_card_bg)
                setChipStrokeColorResource(R.color.home_card_stroke)
                chipStrokeWidth = 3f * density
                
                // 크기 대폭 축소 (두 줄/여러 줄 배치를 위해)
                minHeight = (56 * density).toInt()
                chipStartPadding = 20 * density
                chipEndPadding = 20 * density
                
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(28 * density)
                    .build()

                setOnClickListener { handleOptionClick(index) }
            }
            binding.layoutOptionsChips.addView(chip)
        }
    }

    private fun setHighlightedText(view: TextView, text: String, typeLabel: String) {
        val spannable = SpannableString(text)
        
        // 1. 유형 라벨 하이라이트 (Mint)
        val typeEnd = typeLabel.length + 2
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#00FFB2")),
            0,
            typeEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 2. 따옴표 안의 텍스트 하이라이트 (Yellow)
        val start = text.indexOf("\"")
        val end = text.lastIndexOf("\"")
        
        if (start != -1 && end != -1 && start < end) {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFD600")),
                start,
                end + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        view.text = spannable
    }

    private fun handleOptionClick(optionIndex: Int) {
        if (isSolutionMode) return

        val q = questions[currentQuestionIndex]
        val isCorrect = optionIndex == q.correctAnswerIndex
        userAnswers[currentQuestionIndex] = optionIndex

        disableOptions()

        val selectedAnswer = q.options[optionIndex]

        if (q.type == "FILL") {
            val originalText = "[${q.typeLabel}] ${q.question}"
            val replacedText = when {
                originalText.contains("_____") -> originalText.replace("_____", " $selectedAnswer ")
                originalText.contains("[      ]") -> originalText.replace("[      ]", " $selectedAnswer ")
                else -> originalText
            }
            // 텍스트를 그냥 설정하지 않고, 하이라이트를 유지한 채로 업데이트
            setHighlightedText(binding.tvQuizQuestion, replacedText, q.typeLabel)
        }

        if (q.type == "OX") {
            val selectedView = if (optionIndex == 0) binding.btnOxO else binding.btnOxX
            selectedView.isSelected = true
        } else if (q.type == "FILL" || q.type == "SITUATION") {
            val chip = binding.layoutOptionsChips.getChildAt(optionIndex) as? com.google.android.material.chip.Chip
            chip?.let {
                it.setChipBackgroundColorResource(R.color.quiz_mint)
                it.setTextColor(Color.parseColor("#0A1633"))
            }
        } else {
            val selectedView = when(optionIndex) {
                0 -> { binding.ivCheck1.visibility = View.VISIBLE; binding.btnDummyOpt1 }
                1 -> { binding.ivCheck2.visibility = View.VISIBLE; binding.btnDummyOpt2 }
                2 -> { binding.ivCheck3.visibility = View.VISIBLE; binding.btnDummyOpt3 }
                else -> { binding.ivCheck4.visibility = View.VISIBLE; binding.btnDummyOpt4 }
            }
            selectedView.isSelected = true
        }

        view?.postDelayed({
            if (!isAdded) return@postDelayed

            if (isCorrect) {
                correctCount++
                goToNextQuestion()
            } else {
                wrongCount++
                lives--
                updateHearts(animate = true)
                shakeView(binding.layoutHearts)
                
                if (lives <= 0) {
                    showMissionFailure()
                } else {
                    goToNextQuestion()
                }
            }
        }, 500)
    }

    private fun goToNextQuestion() {
        if (currentQuestionIndex < totalQuestions - 1) {
            currentQuestionIndex++
            resetOptions()
            setupQuizUI()
        } else {
            showResultOverlay(isSuccess = true)
        }
    }

    private fun disableOptions() {
        binding.btnDummyOpt1.isEnabled = false
        binding.btnDummyOpt2.isEnabled = false
        binding.btnDummyOpt3.isEnabled = false
        binding.btnDummyOpt4.isEnabled = false
        binding.btnOxO.isEnabled = false
        binding.btnOxX.isEnabled = false
        for (i in 0 until binding.layoutOptionsChips.childCount) {
            binding.layoutOptionsChips.getChildAt(i).isEnabled = false
        }
    }

    private fun showMissionFailure() {
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = "❌ 미션 실패..."
        binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
        binding.tvFeedbackContent.text = "오답을 3번이나 했어요. 아쉽지만 이번 미션은 여기서 멈춰야 할 것 같아요. 다시 도전해볼까요?"
        binding.btnNextQuestion.text = "결과 확인하기"
        binding.btnNextQuestion.setOnClickListener {
            showResultOverlay(isSuccess = false)
        }
    }

    private fun showResultOverlay(isSuccess: Boolean) {
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        val passThreshold = 8
        val isPassed = correctCount >= passThreshold && isSuccess

        binding.tvResultTitle.text = if (isPassed) "미션 성공!" else "조금 더 노력해봐!"
        binding.tvResultTitle.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        
        binding.tvResultScore.text = "$correctCount / $totalQuestions"
        binding.tvResPassStatus.text = if (isPassed) "PASS" else "FAIL"
        binding.tvResPassStatus.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))

        // 오답 수 표시 처리
        if (isPassed) {
            binding.layoutResWrong.visibility = View.GONE
        } else {
            binding.layoutResWrong.visibility = View.VISIBLE
            binding.tvWrongCount.text = "${totalQuestions - correctCount}개"
        }
        
        // XP 및 보너스 계산 (v2.3)
        val baseXp = if (isPassed) 10 else 0
        val perfectBonus = if (correctCount == 10) 10 else 0
        val petBonusPercent = if (isPassed) 15 else 0
        val petBonusXp = if (isPassed) 10 else 0 // 고정값 예시
        
        val totalXp = baseXp + perfectBonus + petBonusXp
        
        binding.tvResultXp.text = "+$totalXp XP"
        binding.tvResPetBonus.text = "+$petBonusPercent% XP"
        
        // 버튼 텍스트 및 상태 조절
        if (isPassed) {
            binding.btnRetryQuiz.visibility = View.GONE
            binding.btnFinish.text = "학습 완료"
        } else {
            binding.btnRetryQuiz.visibility = View.VISIBLE
            binding.btnRetryQuiz.text = "다시 도전하기"
            binding.btnFinish.text = "학습 완료"
        }

        // 펫 애니메이션
        binding.lavResultPet.setAnimation(if (isPassed) R.raw.pet_idle else R.raw.pet_idle) // 실패 시 다른 애니메이션 있으면 교체 가능
        binding.lavResultPet.playAnimation()

        if (correctCount == 10) {
            showEvolutionCelebration()
        }
    }

    private fun showEvolutionCelebration() {
        android.widget.Toast.makeText(requireContext(), "✨ 아이몽 진화 달성! (v2.3) ✨", android.widget.Toast.LENGTH_LONG).show()
    }

    private fun navigateToResult(isSuccess: Boolean) {
        // 기존 네비게이션은 유지하되, 오버레이 사용을 우선함
        val action = DummyQuizFragmentDirections.actionDummyQuizFragmentToDummyTestFragment(
            score = correctCount,
            isSuccess = isSuccess,
            wrongCount = wrongCount
        )
        findNavController().navigate(action)
    }

    private fun resetOptions() {
        binding.btnDummyOpt1.isEnabled = true
        binding.btnDummyOpt2.isEnabled = true
        binding.btnDummyOpt3.isEnabled = true
        binding.btnDummyOpt4.isEnabled = true
        binding.btnOxO.isEnabled = true
        binding.btnOxX.isEnabled = true
        
        binding.btnDummyOpt1.isSelected = false
        binding.btnDummyOpt2.isSelected = false
        binding.btnDummyOpt3.isSelected = false
        binding.btnDummyOpt4.isSelected = false
        binding.btnOxO.isSelected = false
        binding.btnOxX.isSelected = false
        
        val defaultStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70"))
        binding.btnOxO.setStrokeColor(defaultStrokeColor)
        binding.btnOxO.setStrokeWidth(2)
        binding.btnOxX.setStrokeColor(defaultStrokeColor)
        binding.btnOxX.setStrokeWidth(2)
        
        binding.btnDummyOpt1.setStrokeColor(defaultStrokeColor)
        binding.btnDummyOpt1.setStrokeWidth(2)
        binding.btnDummyOpt2.setStrokeColor(defaultStrokeColor)
        binding.btnDummyOpt2.setStrokeWidth(2)
        binding.btnDummyOpt3.setStrokeColor(defaultStrokeColor)
        binding.btnDummyOpt3.setStrokeWidth(2)
        binding.btnDummyOpt4.setStrokeColor(defaultStrokeColor)
        binding.btnDummyOpt4.setStrokeWidth(2)
        
        binding.ivCheck1.visibility = View.GONE
        binding.ivCheck2.visibility = View.GONE
        binding.ivCheck3.visibility = View.GONE
        binding.ivCheck4.visibility = View.GONE

        binding.btnNextQuestion.text = "다음 문제 →"
        binding.btnFeedbackRetry.visibility = View.VISIBLE
    }

    private fun updateHearts(animate: Boolean) {
        val hearts = listOf(binding.ivHeart1, binding.ivHeart2, binding.ivHeart3)
        
        hearts.forEachIndexed { index, imageView ->
            val isFilled = index < lives
            val targetRes = if (isFilled) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
            
            if (animate && index == lives) {
                animateHeartLoss(imageView)
            } else {
                imageView.setImageResource(targetRes)
            }
        }
    }

    private fun animateHeartLoss(view: ImageView) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1.2f, 0.0f),
            PropertyValuesHolder.ofFloat("scaleY", 1.2f, 0.0f)
        )
        scaleDown.duration = 400
        scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.setImageResource(R.drawable.ic_heart_empty)
                ObjectAnimator.ofPropertyValuesHolder(
                    view,
                    PropertyValuesHolder.ofFloat("scaleX", 0.0f, 1.0f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.0f, 1.0f)
                ).setDuration(300).start()
            }
        })
        scaleDown.start()
    }

    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f)
        shake.duration = 500
        shake.interpolator = CycleInterpolator(3f)
        shake.start()
    }

    private fun showFeedback(isCorrect: Boolean) {
        val q = questions[currentQuestionIndex]
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        if (isCorrect) {
            binding.tvFeedbackTitle.text = "🎉 정답이에요! +10 EXP"
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#00FFB2"))
            binding.tvFeedbackContent.text = q.feedback
        } else {
            binding.tvFeedbackTitle.text = "😥 아쉬워요! 다시 생각해볼까?"
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
            binding.tvFeedbackContent.text = q.feedback
        }
    }

    override fun initObserver() {}
}
