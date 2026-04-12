package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.databinding.ItemHomeQuestBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()

    override fun initView() {
        // 뽑기 버튼 클릭
        binding.btnGacha.setOnClickListener {
            // TODO: 가챠 화면 이동
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: HomeUiState) {
        with(binding) {
            // 1. 상단 상태바 (시안 v1.3)
            tvStreakBadge.text = "🔥 ${state.streakDays}일 연속!"
            tvLevelBadge.text = "🔍 Lv.${state.userLevel} ${viewModel.getProfileLabel(state.profileType)}"
            
            // 2. 캐릭터 및 메시지
            tvPetMessage.text = state.petMessage
            tvPetNameLevel.text = "${state.petName} Lv.${state.petLevel}"
            
            // 3. 경험치 바 (그라데이션 적용됨)
            tvPetXpLabel.text = "${state.petXp} / ${state.petMaxXp} EXP"
            pbXp.max = state.petMaxXp
            pbXp.progress = state.petXp
            
            // 4. 오늘의 퀘스트 진행도 배지
            tvQuestProgressBadge.text = "${state.todayQuestProgress} 완료"
            
            // 5. 퀘스트 리스트 동적 생성
            setupQuestList(state.quests)
            
            // 6. 가챠 배너
            tvGachaTitle.text = "가챠 티켓 ${state.normalTickets}장 보유!"
            tvGachaDesc.text = state.gachaDescription
            
            if (!lottiePet.isAnimating) lottiePet.playAnimation()
        }
    }

    private fun setupQuestList(quests: List<QuestItemUiState>) {
        binding.layoutQuestList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        quests.forEach { quest ->
            val itemBinding = ItemHomeQuestBinding.inflate(inflater, binding.layoutQuestList, false)
            with(itemBinding) {
                tvQuestTitle.text = quest.title
                tvQuestReward.text = quest.rewardSummary
                
                if (quest.isCompleted) {
                    ivCompleted.visibility = View.VISIBLE
                    tvStartBtn.visibility = View.GONE
                } else {
                    ivCompleted.visibility = View.GONE
                    tvStartBtn.visibility = if (quest.canStart) View.VISIBLE else View.GONE
                }
                
                root.setOnClickListener {
                    if (quest.canStart) { /* 학습 화면 이동 */ }
                }
            }
            binding.layoutQuestList.addView(itemBinding.root)
        }
    }
}
