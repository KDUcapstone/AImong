package com.kduniv.aimong.feature.mission.presentation

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentDummyTestBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DummyTestFragment : BaseFragment<FragmentDummyTestBinding>(FragmentDummyTestBinding::inflate) {

    private val args: DummyTestFragmentArgs by navArgs()

    override fun initView() {
        val score = args.score
        val isSuccess = args.isSuccess
        val wrongCount = args.wrongCount

        // 펫 등급에 따른 보너스 XP 계산 (명세서 기준)
        // 시뮬레이션을 위해 "RARE" 등급으로 가정 (RARE는 1오답 이하 시 보너스)
        val petGrade = "RARE" 
        val bonusXp = calculatePetBonus(petGrade, wrongCount)

        if (isSuccess) {
            setupSuccessUI(score, bonusXp, petGrade)
        } else {
            setupFailureUI(score)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack(com.kduniv.aimong.R.id.learningFragment, false)
        }

        binding.btnRetry.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun calculatePetBonus(grade: String, wrongCount: Int): Int {
        return when (grade) {
            "NORMAL" -> if (wrongCount == 0) 10 else 0
            "RARE" -> if (wrongCount <= 1) 10 else 0
            "EPIC" -> if (wrongCount <= 2) 10 else 0
            "LEGEND" -> if (wrongCount <= 2) 15 else 0
            else -> 0
        }
    }

    private fun setupSuccessUI(score: Int, bonusXp: Int, petGrade: String) {
        binding.tvResultStatus.text = "미션 성공!"
        binding.tvResultStatus.setTextColor(android.graphics.Color.parseColor("#00FFB2"))
        binding.tvResultSub.text = "정말 대단해! 리터러시 박사가 다 됐는걸?"
        
        val baseXp = 10
        val perfectBonus = if (score == 10) 10 else 0
        val totalXp = baseXp + perfectBonus + bonusXp

        binding.tvCorrectCount.text = "$score / 10"
        binding.tvPassStatus.text = "PASS"
        
        val bonusDetail = StringBuilder()
        if (perfectBonus > 0) bonusDetail.append("퍼펙트 +10 ")
        if (bonusXp > 0) bonusDetail.append("펫($petGrade) +$bonusXp")
        binding.tvPetBonus.text = if (bonusDetail.isEmpty()) "보너스 없음" else bonusDetail.toString()
        
        binding.tvXpGain.text = "+$totalXp XP"
        
        binding.layoutRewards.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE

        if (score == 10) {
            binding.root.postDelayed({
                showEvolutionCelebration()
            }, 1000)
        }
        
        binding.pbXpProgress.progress = 30
        ObjectAnimator.ofInt(binding.pbXpProgress, "progress", 30, 80)
            .setDuration(1500)
            .start()
    }

    private fun setupFailureUI(score: Int) {
        binding.tvResultStatus.text = "조금 더 노력해봐!"
        binding.tvResultStatus.setTextColor(android.graphics.Color.parseColor("#FF4B4B"))
        binding.tvResultSub.text = "오답을 3번이나 했어요. 아쉽지만 이번 미션은 여기서 멈춰야 할 것 같아요. 다시 도전해볼까요?"
        
        binding.tvCorrectCount.text = "$score / 10"
        binding.tvPassStatus.text = "FAIL"
        binding.tvPetBonus.text = "+0 XP"

        binding.tvXpGain.text = "+0 XP"
        
        binding.layoutRewards.visibility = View.GONE
        binding.btnRetry.visibility = View.VISIBLE
        
        binding.lavDummyPet.setAnimation(com.kduniv.aimong.R.raw.pet_idle)
    }

    private fun showEvolutionCelebration() {
        android.widget.Toast.makeText(requireContext(), "✨ 아이몽 진화 달성! (v2.3) ✨", android.widget.Toast.LENGTH_LONG).show()
    }

    override fun initObserver() {}
}
