package com.kduniv.aimong.feature.dev.mock

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentQuizBinding

class DummyQuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private var currentQuestionIndex = 0
    private var correctCount = 0
    private var lives = 3
    private var timer: CountDownTimer? = null
    private var maxPlayedIndex = 0
    private var isReviewMode = false

    private data class DummyQuestion(
        val type: String, // "OX", "MULTIPLE_CHOICE", "FILL", "SITUATION"
        val text: String,
        val options: List<String> = emptyList(),
        val answer: String,
        val explanation: String
    )

    private val questions = listOf(
        DummyQuestion("OX", "[목업] AI는 데이터를 통해 학습한다.", answer = "O", explanation = "AI는 방대한 데이터를 분석하여 패턴을 학습합니다."),
        DummyQuestion("MULTIPLE_CHOICE", "[목업] 다음 중 생성형 AI가 아닌 것은?", listOf("ChatGPT", "Claude", "Midjourney", "전자계산기"), "전자계산기", "전자계산기는 사전에 프로그래밍된 단순 연산만 수행합니다."),
        DummyQuestion("FILL", "[목업] AI가 사실이 아닌 정보를 그럴듯하게 지어내는 현상을 [      ](이)라고 합니다.", listOf("할루시네이션", "딥러닝", "오버피팅", "파인튜닝"), "할루시네이션", "환각(Hallucination) 현상이라고 부릅니다."),
        DummyQuestion("SITUATION", "[목업] 모르는 번호로 온 문자에 단축 URL이 있습니다. 어떻게 해야 할까요?", listOf("무시한다", "링크를 누른다", "답장을 보낸다", "주변인에게 공유한다"), "무시한다", "출처가 불분명한 링크는 스미싱 위험이 있으므로 무시해야 합니다."),
        DummyQuestion("OX", "[목업] 개인정보는 AI 챗봇에게 마음대로 알려줘도 된다.", answer = "X", explanation = "AI 챗봇에게 개인정보를 입력하면 학습 데이터로 노출될 위험이 있습니다."),
        DummyQuestion("MULTIPLE_CHOICE", "[목업] 비밀번호를 설정할 때 가장 안전한 방법은?", listOf("생년월일 사용", "123456 사용", "영문/숫자/특수문자 조합", "전화번호 사용"), "영문/숫자/특수문자 조합", "복잡한 조합을 사용해야 보안이 강화됩니다."),
        DummyQuestion("FILL", "[목업] 인간의 뇌 신경망을 모방한 기계 학습 방법을 [      ](이)라고 합니다.", listOf("딥러닝", "블록체인", "메타버스", "클라우드"), "딥러닝", "딥러닝은 인공신경망 기반의 강력한 기계 학습 기술입니다."),
        DummyQuestion("SITUATION", "[목업] 친구가 AI로 만든 가짜 뉴스를 공유했습니다. 올바른 대처는?", listOf("더 널리 퍼뜨린다", "가짜 뉴스일 수 있다고 알려준다", "그대로 믿는다", "화낸다"), "가짜 뉴스일 수 있다고 알려준다", "가짜 뉴스의 확산을 막고 사실 여부를 확인하도록 돕는 것이 중요합니다."),
        DummyQuestion("OX", "[목업] AI가 만든 글이나 그림은 아무 조건 없이 상업적으로 팔아도 된다.", answer = "X", explanation = "AI 결과물도 약관이나 저작권 문제가 발생할 수 있어 주의가 필요합니다."),
        DummyQuestion("MULTIPLE_CHOICE", "[목업] 다음 중 개인정보에 해당하지 않는 것은?", listOf("주민등록번호", "휴대폰 번호", "좋아하는 과일", "집 주소"), "좋아하는 과일", "단순히 좋아하는 과일 자체만으로는 특정 개인을 식별하기 어렵습니다.")
    )

    override fun initView() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnResFinish.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnReportQuestion.setOnClickListener {
            Toast.makeText(requireContext(), "문제 신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnResRetry.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            currentQuestionIndex = 0
            maxPlayedIndex = 0
            correctCount = 0
            lives = 3
            isReviewMode = true // 복습 모드 진입
            binding.cardReviewBadge.visibility = View.VISIBLE
            updateHearts()
            showQuestion(0)
        }
        
        binding.btnResViewSolutions.setOnClickListener {
            // 풀이 보기 목업: 1번 문제로 돌아가 해설을 보여주며 퀴즈를 종료하는 형태
            binding.layoutQuizResult.visibility = View.GONE
            currentQuestionIndex = 0
            showSolutionMock(0)
        }

        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                showQuestion(currentQuestionIndex)
            } else {
                showResult()
            }
        }
        
        // OX 옵션 리스너
        binding.btnOxO.setOnClickListener { checkAnswer("O") }
        binding.btnOxX.setOnClickListener { checkAnswer("X") }
        
        // 객관식 4선지 리스너
        val optionViews = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        optionViews.forEachIndexed { index, tv ->
            (tv.parent as View).setOnClickListener {
                checkAnswer(questions[currentQuestionIndex].options[index])
            }
        }
        
        showQuestion(currentQuestionIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTimer.text = "⏱ ${millisUntilFinished / 1000}초 남음"
            }
            override fun onFinish() {
                binding.tvTimer.text = "⏱ 0초 남음"
                if (binding.layoutFeedbackPanel.visibility != View.VISIBLE) {
                    checkAnswer("") // Time's up -> treated as wrong answer
                }
            }
        }.start()
    }

    private fun updateHearts() {
        val emptyHeart = com.kduniv.aimong.R.drawable.ic_heart_empty
        val filledHeart = com.kduniv.aimong.R.drawable.ic_heart_filled
        binding.ivHeart1.setImageResource(if (lives >= 1) filledHeart else emptyHeart)
        binding.ivHeart2.setImageResource(if (lives >= 2) filledHeart else emptyHeart)
        binding.ivHeart3.setImageResource(if (lives >= 3) filledHeart else emptyHeart)
    }

    private fun shakeScreen() {
        val shakeCard = ObjectAnimator.ofFloat(binding.layoutQuestionCard, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        val shakeOptions = ObjectAnimator.ofFloat(binding.layoutOptionsContainer, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shakeCard.duration = 500
        shakeOptions.duration = 500
        shakeCard.start()
        shakeOptions.start()
    }

    private fun showQuestion(index: Int) {
        binding.layoutQuestionCard.visibility = View.VISIBLE
        binding.layoutFeedbackPanel.visibility = View.GONE
        
        startTimer()
        updateHearts()

        val q = questions[index]
        binding.tvQuizQuestion.text = q.text
        binding.tvQuestionCount.text = "${index + 1} / ${questions.size} 문제"
        binding.pbQuizProgress.progress = index + 1
        binding.pbQuizProgress.max = questions.size

        // 버튼 초기화
        binding.layoutOptionsContainer.visibility = View.VISIBLE
        binding.layoutOptionsStandard.visibility = View.GONE
        binding.layoutOptionsOx.visibility = View.GONE
        binding.layoutOptionsChips.visibility = View.GONE
        binding.layoutOptionsChips.removeAllViews()

        when (q.type) {
            "OX" -> {
                binding.layoutOptionsOx.visibility = View.VISIBLE
                binding.btnOxO.setStrokeWidth(0)
                binding.btnOxX.setStrokeWidth(0)
            }
            "MULTIPLE_CHOICE" -> {
                binding.layoutOptionsStandard.visibility = View.VISIBLE
                val optionViews = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
                q.options.forEachIndexed { i, optText ->
                    if (i < optionViews.size) {
                        optionViews[i].text = optText
                        val card = optionViews[i].parent.parent as com.google.android.material.card.MaterialCardView
                        card.setCardBackgroundColor(Color.parseColor("#1A2B52"))
                        card.strokeWidth = 0
                    }
                }
            }
            "FILL", "SITUATION" -> {
                binding.layoutOptionsChips.visibility = View.VISIBLE
                q.options.forEach { optText ->
                    val chip = Chip(requireContext()).apply {
                        text = optText
                        isCheckable = false
                        checkedIcon = null
                        isClickable = true
                        setChipBackgroundColorResource(com.kduniv.aimong.R.color.home_card_bg)
                        setTextColor(Color.WHITE)
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#243B70"))
                        chipStrokeWidth = 2f
                        setOnClickListener { 
                            setChipBackgroundColorResource(com.kduniv.aimong.R.color.quiz_mint)
                            setTextColor(Color.BLACK)
                            checkAnswer(optText) 
                        }
                    }
                    binding.layoutOptionsChips.addView(chip)
                }
            }
        }
    }

    private fun checkAnswer(userAnswer: String) {
        timer?.cancel()
        
        // 최고 도달 인덱스 갱신 (풀이 보기 범위 제한용)
        if (currentQuestionIndex > maxPlayedIndex) {
            maxPlayedIndex = currentQuestionIndex
        }
        
        val q = questions[currentQuestionIndex]
        val isCorrect = userAnswer == q.answer
        if (isCorrect) {
            correctCount++
        } else {
            lives--
            updateHearts()
            shakeScreen()
        }

        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        
        // 정답 효과 (간단 목업)
        if (q.type == "OX") {
            if (userAnswer == "O") binding.btnOxO.setStrokeWidth(4)
            if (userAnswer == "X") binding.btnOxX.setStrokeWidth(4)
        }
        
        if (q.type == "FILL" && userAnswer.isNotBlank()) {
            val filledText = q.text.replace("[      ]", "[ $userAnswer ]")
            binding.tvQuizQuestion.text = filledText
        }
        
        if (isCorrect) {
            binding.tvFeedbackTitle.text = "정답입니다! 🎉"
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#00FFB2"))
        } else {
            binding.tvFeedbackTitle.text = "아쉬워요! 😢"
            binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FF4B4B"))
        }
        
        binding.tvFeedbackContent.text = q.explanation
        
        val isFailedByLives = lives <= 0
        val isLast = currentQuestionIndex == questions.size - 1
        
        if (isFailedByLives) {
            binding.btnNextQuestion.text = "결과 보기"
        } else {
            binding.btnNextQuestion.text = if (isLast) "결과 보기" else "다음 문제 →"
        }
        
        // 버튼 클릭을 가로채서 생명력이 0이면 결과로 바로 이동하도록 수정
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            if (isFailedByLives) {
                showResult()
            } else {
                currentQuestionIndex++
                if (currentQuestionIndex < questions.size) {
                    showQuestion(currentQuestionIndex)
                } else {
                    showResult()
                }
            }
        }
    }

    private fun showResult() {
        timer?.cancel()
        binding.layoutQuestionCard.visibility = View.GONE
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.layoutOptionsContainer.visibility = View.GONE
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        // Update review badge visibility
        binding.cardReviewBadge.visibility = if (isReviewMode) View.VISIBLE else View.GONE
        
        // API 명세대로 3번 틀리면 실패
        val isPassed = correctCount >= 8 && lives > 0
        
        binding.tvResultStatus.text = if (isPassed) "미션 성공!" else "미션 실패"
        binding.tvResultStatus.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        binding.tvResultSub.text = if (isPassed) "정말 대단해! 모두 통과했어!" else "아쉽게 탈락했어. 다시 도전해보자!"
        
        binding.tvResCorrectCount.text = "$correctCount / ${maxPlayedIndex + 1}"
        binding.tvResPassStatus.text = if (isPassed) "PASS" else "FAIL"
        binding.tvResPassStatus.setTextColor(if (isPassed) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        
        binding.tvResXpGain.text = if (isPassed) {
            if (isReviewMode) "+ 20 XP" else "+ 100 XP"
        } else {
            if (isReviewMode) "+ 0 XP" else "+ 20 XP"
        }
        
        if (!isPassed) {
            binding.layoutWrongStat.visibility = View.VISIBLE
            binding.layoutPetBonusStat.visibility = View.GONE
            binding.tvResWrongCount.text = "${maxPlayedIndex + 1 - correctCount}개"
            binding.btnResRetry.visibility = View.VISIBLE
            binding.btnResFinish.text = "다음에 하기"
            binding.btnResViewSolutions.visibility = View.VISIBLE
        } else {
            binding.layoutWrongStat.visibility = View.GONE
            binding.layoutPetBonusStat.visibility = View.VISIBLE
            binding.btnResRetry.visibility = View.GONE
            binding.btnResFinish.text = "학습 완료"
            binding.btnResViewSolutions.visibility = View.VISIBLE
        }
    }
    
    private fun showSolutionMock(index: Int) {
        showQuestion(index)
        val q = questions[index]
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = "풀이 모드"
        binding.tvFeedbackTitle.setTextColor(Color.parseColor("#FFD600"))
        binding.tvFeedbackContent.text = "정답: ${q.answer}\n\n해설: ${q.explanation}"
        
        // 실패했다면 진행한 곳까지만, 성공했다면 끝까지
        val targetSize = if (lives <= 0) maxPlayedIndex else questions.size - 1
        val isLast = index >= targetSize
        
        binding.btnNextQuestion.text = if (isLast) "결과로 돌아가기" else "다음 풀이 →"
        
        // OnClickListener 교체로 목업 풀이 모드 순회
        binding.btnNextQuestion.setOnClickListener {
            if (isLast) {
                showResult()
                binding.btnNextQuestion.setOnClickListener {
                    binding.layoutFeedbackPanel.visibility = View.GONE
                    if (lives <= 0) {
                        showResult()
                    } else {
                        currentQuestionIndex++
                        if (currentQuestionIndex < questions.size) showQuestion(currentQuestionIndex) else showResult()
                    }
                }
            } else {
                showSolutionMock(index + 1)
            }
        }
    }

    override fun initObserver() {}
}
