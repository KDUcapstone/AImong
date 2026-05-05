package com.kduniv.aimong.feature.dev.mock

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.CountDownTimer
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.kduniv.aimong.R
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
        val type: String,
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
        DummyQuestion("OX", "[목업] 개인정보는 AI 챗봇에게 마음대로 알려줘도 된다.", answer = "X", explanation = "AI 챗봇에게 개인정보를 입력하면 학습 데이터로 노출될 위험이 있습니다.")
    )

    override fun initView() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnResFinish.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnReportQuestion.setOnClickListener { showQuestionReportReasonDialog() }
        
        binding.btnResRetry.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            currentQuestionIndex = 0
            maxPlayedIndex = 0
            correctCount = 0
            lives = 3
            isReviewMode = true
            binding.cardReviewBadge.visibility = View.VISIBLE
            updateHearts()
            showQuestion(0)
        }
        
        binding.btnResViewSolutions.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
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
        
        binding.btnOxO.setOnClickListener { checkAnswer("O") }
        binding.btnOxX.setOnClickListener { checkAnswer("X") }
        
        val optionViews = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        optionViews.forEachIndexed { index, tv ->
            (tv.parent as View).setOnClickListener {
                if (currentQuestionIndex < questions.size) {
                    checkAnswer(questions[currentQuestionIndex].options[index])
                }
            }
        }
        
        showQuestion(currentQuestionIndex)
    }

    override fun onDestroyView() {
        timer?.cancel()
        timer = null
        super.onDestroyView()
    }

    private fun startTimer() {
        timer?.cancel()
        if (_binding == null) return
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                binding.tvTimer.text = "⏱ ${millisUntilFinished / 1000}초 남음"
            }
            override fun onFinish() {
                if (_binding == null) return
                binding.tvTimer.text = "⏱ 0초 남음"
                if (binding.layoutFeedbackPanel.visibility != View.VISIBLE) {
                    checkAnswer("")
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

    private fun showQuestion(index: Int) {
        if (_binding == null) return
        binding.layoutQuestionCard.visibility = View.VISIBLE
        binding.layoutFeedbackPanel.visibility = View.GONE
        
        startTimer()
        updateHearts()

        val q = questions[index]
        binding.tvQuizQuestion.text = q.text
        binding.tvQuestionCount.text = "${index + 1} / ${questions.size} 문제"
        binding.pbQuizProgress.progress = index + 1
        binding.pbQuizProgress.max = questions.size

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
                val density = resources.displayMetrics.density
                q.options.forEach { optText ->
                    val chip = Chip(requireContext()).apply {
                        text = optText
                        setTextColor(Color.WHITE)
                        setOnClickListener { checkAnswer(optText) }
                    }
                    binding.layoutOptionsChips.addView(chip)
                }
            }
        }
    }

    private fun checkAnswer(userAnswer: String) {
        timer?.cancel()
        if (currentQuestionIndex > maxPlayedIndex) maxPlayedIndex = currentQuestionIndex
        
        val q = questions[currentQuestionIndex]
        val isCorrect = userAnswer == q.answer
        if (isCorrect) correctCount++ else { lives--; updateHearts() }

        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = if (isCorrect) "정답입니다! 🎉" else "아쉽워요! 😢"
        binding.tvFeedbackContent.text = q.explanation
        
        val isFailedByLives = lives <= 0
        val isLast = currentQuestionIndex == questions.size - 1
        
        binding.btnNextQuestion.text = if (isFailedByLives || isLast) "결과 보기" else "다음 문제 →"
        binding.btnNextQuestion.setOnClickListener {
            binding.layoutFeedbackPanel.visibility = View.GONE
            if (isFailedByLives || isLast) showResult() else {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            }
        }
    }

    private fun showResult() {
        binding.layoutQuestionCard.visibility = View.GONE
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.layoutOptionsContainer.visibility = View.GONE
        binding.layoutQuizResult.visibility = View.VISIBLE
        
        val isPassed = lives > 0
        binding.tvResultStatus.text = if (isPassed) "미션 성공!" else "미션 실패"
        binding.tvResCorrectCount.text = "$correctCount / ${maxPlayedIndex + 1}"
    }

    private fun showSolutionMock(index: Int) {
        showQuestion(index)
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = "풀이 모드"
        val isLast = index >= maxPlayedIndex
        binding.btnNextQuestion.text = if (isLast) "결과로 돌아가기" else "다음 풀이 →"
        binding.btnNextQuestion.setOnClickListener { if (isLast) showResult() else showSolutionMock(index + 1) }
    }

    override fun initObserver() {}

    private fun showQuestionReportReasonDialog() {
        Toast.makeText(requireContext(), "신고 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
}
