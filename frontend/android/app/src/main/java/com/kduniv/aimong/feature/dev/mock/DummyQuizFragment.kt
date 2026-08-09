package com.kduniv.aimong.feature.dev.mock

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentQuizBinding
import com.kduniv.aimong.feature.quiz.presentation.MissionHeartGrade

class DummyQuizFragment : BaseFragment<FragmentQuizBinding>(FragmentQuizBinding::inflate) {

    private var currentQuestionIndex = 0
    private var correctCount = 0
    private var lives = 3
    private var totalHeartsLost = 0
    private var timer: CountDownTimer? = null
    private var questionTimeLeftMs: Long = 30000L
    private var maxPlayedIndex = 0
    private var isReviewMode = false
    private var isSolutionMode = false
    private val answerSnapshotByIndex = mutableMapOf<Int, String>()

    private data class DummyQuestion(
        val type: String,
        val text: String,
        val options: List<String> = emptyList(),
        val answer: String,
        val explanation: String
    )

    private val questions = listOf(
        DummyQuestion("OX", "AI는 데이터를 통해 학습한다.", answer = "O", explanation = "AI는 방대한 데이터를 분석하여 패턴을 학습합니다."),
        DummyQuestion("MULTIPLE_CHOICE", "다음 중 생성형 AI가 아닌 것은?", listOf("ChatGPT", "Claude", "Midjourney", "전자계산기"), "전자계산기", "전자계산기는 사전에 프로그래밍된 단순 연산만 수행합니다."),
        DummyQuestion("FILL", "AI가 사실이 아닌 정보를 그럴듯하게 지어내는 현상을 [      ](이)라고 합니다.", listOf("할루시네이션", "딥러닝", "오버피팅", "파인튜닝"), "할루시네이션", "환각(Hallucination) 현상이라고 부릅니다."),
        DummyQuestion("SITUATION", "모르는 번호로 온 문자에 단축 URL이 있습니다. 어떻게 해야 할까요?", listOf("무시한다", "링크를 누른다", "답장을 보낸다", "주변인에게 공유한다"), "무시한다", "출처가 불분명한 링크는 스미싱 위험이 있으므로 무시해야 합니다."),
        DummyQuestion("OX", "개인정보는 AI 챗봇에게 마음대로 알려줘도 된다.", answer = "X", explanation = "AI 챗봇에게 개인정보를 입력하면 학습 데이터로 노출될 위험이 있습니다.")
    )

    override fun initView() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnResFinish.setOnClickListener { findNavController().popBackStack() }

        binding.btnResRetry.setOnClickListener {
            binding.layoutQuizResult.visibility = View.GONE
            currentQuestionIndex = 0
            maxPlayedIndex = 0
            correctCount = 0
            lives = 3
            totalHeartsLost = 0
            isReviewMode = true
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
            if (currentQuestionIndex < questions.size) showQuestion(currentQuestionIndex) else showResult()
        }
        
        binding.btnOxO.setOnClickListener {
            applyOxPendingSelection("O")
            checkAnswer("O")
        }
        binding.btnOxX.setOnClickListener {
            applyOxPendingSelection("X")
            checkAnswer("X")
        }
        
        val optionViews = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        optionViews.forEachIndexed { index, tv ->
            (tv.parent as View).setOnClickListener {
                if (currentQuestionIndex < questions.size) {
                    val allCards = optionViews.map { it.parent.parent as com.google.android.material.card.MaterialCardView }
                    allCards.forEachIndexed { i, card ->
                        val isSel = i == index
                        card.setCardBackgroundColor(
                            if (isSel) Color.parseColor("#00FFB2") else Color.parseColor("#1A2B52")
                        )
                        card.strokeWidth = if (isSel) (3 * resources.displayMetrics.density).toInt() else 0
                        card.strokeColor = Color.parseColor("#00FFB2")
                        optionViews[i].setTextColor(if (isSel) Color.parseColor("#0A1633") else Color.WHITE)
                    }
                    checkAnswer((index + 1).toString())
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

    private fun startTimer(reset: Boolean) {
        timer?.cancel()
        if (_binding == null) return
        if (reset) questionTimeLeftMs = 30000L
        timer = object : CountDownTimer(questionTimeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return
                questionTimeLeftMs = millisUntilFinished
                binding.tvTimer.text = "⏱ ${millisUntilFinished / 1000}초 남음"
            }
            override fun onFinish() {
                if (_binding == null) return
                questionTimeLeftMs = 0
                binding.tvTimer.text = "⏱ 0초 남음"
                if (binding.layoutFeedbackPanel.visibility != View.VISIBLE) checkAnswer("")
            }
        }.start()
    }

    private fun applyOxPendingSelection(choice: String) {
        val density = resources.displayMetrics.density
        binding.btnOxO.strokeColor = Color.parseColor("#243B70")
        binding.btnOxO.strokeWidth = (1 * density).toInt()
        binding.btnOxX.strokeColor = Color.parseColor("#243B70")
        binding.btnOxX.strokeWidth = (1 * density).toInt()

        val selected = if (choice == "O") binding.btnOxO else binding.btnOxX
        val color = if (choice == "O") "#00FFB2" else "#FF4B4B"
        selected.strokeColor = Color.parseColor(color)
        selected.strokeWidth = (6 * density).toInt()
    }

    private fun updateHearts() {
        val capped = lives.coerceIn(0, 3)
        val emptyHeart = R.drawable.ic_heart_empty
        val filledHeart = R.drawable.ic_heart_filled
        binding.ivHeart1.setImageResource(if (capped >= 1) filledHeart else emptyHeart)
        binding.ivHeart2.setImageResource(if (capped >= 2) filledHeart else emptyHeart)
        binding.ivHeart3.setImageResource(if (capped >= 3) filledHeart else emptyHeart)
    }

    private fun loseHeart() {
        if (lives > 0) {
            totalHeartsLost++
            lives--
            updateHearts()
        }
    }

    private fun shakeView(v: View) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, 10f).apply {
            duration = 420
            interpolator = android.view.animation.CycleInterpolator(4f)
            start()
        }
    }

    private fun shakeScreen() {
        // 루트를 흔들면 바깥 배경이 비칠 수 있어 컨텐츠만 흔든다.
        shakeView(binding.layoutQuestionCard)
        shakeView(binding.layoutOptionsContainer)
    }

    private fun showQuestion(index: Int) {
        if (_binding == null) return
        binding.layoutQuestionCard.visibility = View.VISIBLE
        binding.layoutFeedbackPanel.visibility = View.GONE
        
        if (!isSolutionMode) startTimer(reset = true) else timer?.cancel()
        updateHearts()

        val q = questions[index]
        val typeLabel = when(q.type) {
            "OX" -> "OX 퀴즈"
            "MULTIPLE_CHOICE" -> "객관식"
            "FILL" -> "단어 채우기"
            "SITUATION" -> "상황 판단"
            else -> "퀴즈"
        }
        setHighlightedText(binding.tvQuizQuestion, "[$typeLabel] ${q.text}")
        binding.tvQuizQuestion.setTextColor(Color.WHITE)

        binding.tvQuestionCount.text = "${index + 1} / ${questions.size} 문제"
        binding.pbQuizProgress.progress = index + 1
        binding.pbQuizProgress.max = questions.size

        binding.layoutOptionsContainer.visibility = View.VISIBLE
        binding.layoutOptionsStandard.visibility = View.GONE
        binding.layoutOptionsOx.visibility = View.GONE
        binding.layoutOptionsChips.visibility = View.GONE
        binding.layoutOptionsChips.removeAllViews()

        val density = resources.displayMetrics.density

        when (q.type) {
            "OX" -> {
                binding.layoutOptionsOx.visibility = View.VISIBLE
                resetOxButtons()
                // 풀이 보기/재진입 시 OX 선택 표시
                answerSnapshotByIndex[index]?.let { selected ->
                    if (selected == "O" || selected == "X") {
                        applyOxPendingSelection(selected)
                    }
                }
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
                        optionViews[i].setTextColor(Color.WHITE)
                    }
                }

                // 풀이 보기/재진입 시에도 내가 고른 보기 표시
                answerSnapshotByIndex[index]?.let { selectedKey ->
                    applyMultipleSelectionMockByKey(selectedKey)
                }
            }
            "FILL", "SITUATION" -> {
                binding.layoutOptionsChips.visibility = View.VISIBLE
                val isSituation = q.type == "SITUATION"
                q.options.forEachIndexed { idx, optText ->
                    val choiceKey = (idx + 1).toString()
                    val chip = Chip(requireContext()).apply {
                        text = optText
                        setTextColor(Color.WHITE)
                        textSize = if (isSituation) 14f else 15f
                        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                        setEnsureMinTouchTargetSize(false)
                        
                        // FILL과 SITUATION 모두 테마 색상(home_card_bg)을 사용하여 회색/흰색 노출 방지
                        setChipBackgroundColorResource(R.color.home_card_bg)
                        setChipStrokeColorResource(R.color.home_card_stroke)
                        chipStrokeWidth = 3f * density

                        setOnClickListener { 
                            // 선택 시 민트색 테마 적용
                            setChipBackgroundColorResource(R.color.quiz_mint)
                            setChipStrokeColorResource(R.color.quiz_mint)
                            setTextColor(Color.WHITE)
                            checkAnswer(
                                choiceKey,
                                fillDisplayWord = if (q.type == "FILL") optText else null
                            )
                        }
                    }
                    binding.layoutOptionsChips.addView(chip)
                }

                // 풀이 보기/재진입 시 칩도 표시
                answerSnapshotByIndex[index]?.let { selectedKey ->
                    val chipIdx = selectedKey.toIntOrNull()?.minus(1) ?: return@let
                    val chip = binding.layoutOptionsChips.getChildAt(chipIdx) as? Chip ?: return@let
                    chip.setChipBackgroundColorResource(R.color.quiz_mint)
                    chip.setChipStrokeColorResource(R.color.quiz_mint)
                    chip.setTextColor(Color.parseColor("#0A1633"))
                }
            }
        }
    }

    private fun applyMultipleSelectionMockByKey(selectedKey: String) {
        val idx = selectedKey.toIntOrNull()?.minus(1) ?: return
        val optionViews = listOf(binding.tvOpt1, binding.tvOpt2, binding.tvOpt3, binding.tvOpt4)
        if (idx !in optionViews.indices) return
        val cards = optionViews.map { it.parent.parent as com.google.android.material.card.MaterialCardView }
        cards.forEachIndexed { i, card ->
            val isSel = i == idx
            card.setCardBackgroundColor(if (isSel) Color.parseColor("#00FFB2") else Color.parseColor("#1A2B52"))
            card.strokeWidth = if (isSel) (3 * resources.displayMetrics.density).toInt() else 0
            card.strokeColor = Color.parseColor("#00FFB2")
            optionViews[i].setTextColor(if (isSel) Color.parseColor("#0A1633") else Color.WHITE)
        }
    }

    private fun expectedAnswerKey(q: DummyQuestion): String {
        return when (q.type) {
            "OX" -> q.answer
            else -> {
                val idx = q.options.indexOfFirst { it == q.answer }
                if (idx >= 0) (idx + 1).toString() else q.answer
            }
        }
    }

    private fun resetOxButtons() {
        val density = resources.displayMetrics.density
        binding.btnOxO.strokeColor = Color.parseColor("#243B70")
        binding.btnOxO.strokeWidth = (1 * density).toInt()
        binding.btnOxX.strokeColor = Color.parseColor("#243B70")
        binding.btnOxX.strokeWidth = (1 * density).toInt()
    }

    private fun setHighlightedText(view: TextView, text: String) {
        val spannable = SpannableString(text)
        val highlightColor = Color.parseColor("#00FFB2")
        val bStart = text.indexOf("[")
        if (bStart != -1) {
            val bEnd = text.indexOf("]", bStart + 1)
            if (bEnd != -1) {
                spannable.setSpan(ForegroundColorSpan(highlightColor), bStart, bEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        view.text = spannable
    }

    private fun checkAnswer(answerForGrade: String, fillDisplayWord: String? = null) {
        timer?.cancel()
        if (currentQuestionIndex > maxPlayedIndex) maxPlayedIndex = currentQuestionIndex
        answerSnapshotByIndex[currentQuestionIndex] = answerForGrade
        
        val q = questions[currentQuestionIndex]
        
        if (q.type == "FILL") {
            val display = fillDisplayWord ?: answerForGrade
            val replacedText = q.text.replace("[      ]", " $display ")
            setHighlightedText(binding.tvQuizQuestion, "[단어 채우기] $replacedText")
        }

        val expected = expectedAnswerKey(q)
        val isCorrect = answerForGrade.isNotEmpty() && answerForGrade == expected
        if (isCorrect) {
            binding.tvQuizQuestion.setTextColor(Color.WHITE)
            correctCount++ 
        } else { 
            binding.tvQuizQuestion.setTextColor(Color.WHITE)
            loseHeart()
            shakeView(binding.layoutHearts)
            shakeScreen()
        }

        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = if (isCorrect) "정답입니다! 🎉" else "아쉬워요! 😢"
        binding.tvFeedbackTitle.setTextColor(if (isCorrect) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B"))
        binding.tvFeedbackContent.text = q.explanation
        
        val isFailedByLives = lives <= 0
        val isLast = currentQuestionIndex == questions.size - 1

        if (isFailedByLives && !isReviewMode) {
            binding.btnNextQuestion.text = getString(R.string.quiz_btn_home)
            binding.btnNextQuestion.setOnClickListener {
                binding.layoutFeedbackPanel.visibility = View.GONE
                Toast.makeText(requireContext(), R.string.quiz_hearts_exhausted_toast, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            binding.btnNextQuestion.text = if (isFailedByLives || isLast) "결과 보기" else "다음 문제 →"
            binding.btnNextQuestion.setOnClickListener {
                binding.layoutFeedbackPanel.visibility = View.GONE
                if (isFailedByLives || isLast) showResult() else {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                }
            }
        }
    }

    private fun showResult() {
        binding.layoutQuestionCard.visibility = View.GONE
        binding.layoutFeedbackPanel.visibility = View.GONE
        binding.layoutOptionsContainer.visibility = View.GONE
        binding.layoutQuizResult.visibility = View.VISIBLE

        val heartsLost = maxOf(totalHeartsLost, (3 - lives.coerceIn(0, 3)).coerceAtLeast(0))
        val grade = MissionHeartGrade.fromHeartsLost(heartsLost)
        val missionCleared = grade != MissionHeartGrade.FAIL

        binding.tvResultStatus.text = when (grade) {
            MissionHeartGrade.PERFECT -> getString(R.string.quiz_result_perfect)
            MissionHeartGrade.SUCCESS -> getString(R.string.quiz_result_success)
            MissionHeartGrade.FAIL -> getString(R.string.quiz_result_fail)
        }
        binding.tvResultStatus.setTextColor(
            if (missionCleared) Color.parseColor("#00FFB2") else Color.parseColor("#FF4B4B")
        )
        binding.tvResultSub.text = when (grade) {
            MissionHeartGrade.PERFECT -> getString(R.string.quiz_result_perfect_subtitle)
            MissionHeartGrade.SUCCESS -> getString(R.string.quiz_result_success_subtitle)
            MissionHeartGrade.FAIL -> "아쉽게 탈락했어. 다시 한 번 도전해볼까?"
        }
        binding.tvResultSub.setTextColor(Color.parseColor("#8A96AD"))

        binding.btnResRetry.visibility = if (missionCleared) View.GONE else View.VISIBLE
    }

    private fun showSolutionMock(index: Int) {
        isSolutionMode = true
        showQuestion(index)
        binding.layoutFeedbackPanel.visibility = View.VISIBLE
        binding.tvFeedbackTitle.text = "풀이 모드"
        val isLast = index >= maxPlayedIndex
        binding.btnNextQuestion.text = if (isLast) "결과로 돌아가기" else "다음 풀이 →"
        binding.btnNextQuestion.setOnClickListener {
            if (isLast) {
                isSolutionMode = false
                showResult()
            } else showSolutionMock(index + 1)
        }
    }

    override fun initObserver() {}
}
