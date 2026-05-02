package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.view.MotionEvent
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.databinding.ItemHomeQuestBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()

    override fun initView() {
        // 뽑기 버튼 클릭 및 애니메이션 적용
        binding.btnGacha.apply {
            setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_gachaFragment)
            }
            setOnScaleTouchListener()
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.root.post { // 뷰가 완전히 그려진 후 업데이트 보장
                        updateUi(state)
                    }
                }
            }
        }
    }

    private fun updateUi(state: HomeUiState) {
        with(binding) {
            // 1. 상단 상태바 (듀오링고 스타일: 이모지 + 숫자만 표시)
            tvStreakCount.text = "🔥 ${state.streakDays}"
            tvUserLevel.text = "🔍 Lv.${state.userLevel} ${viewModel.getProfileLabel(state.profileType)}"
            tvEnergyCount.text = "⚡ ${state.energyCount}"
            tvTicketCount.text = "🎟 ${state.normalTickets}"
            
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
        
        quests.forEachIndexed { index, quest ->
            val itemBinding = ItemHomeQuestBinding.inflate(inflater, binding.layoutQuestList, false)
            with(itemBinding) {
                tvQuestTitle.text = quest.title
                tvQuestReward.text = quest.rewardSummary
                
                // 1. 시안과 동일한 이모지 및 유색 배경 설정
                when(index % 3) {
                    0 -> {
                        tvQuestEmoji.text = "🎒"
                        tvQuestEmoji.setBackgroundResource(R.drawable.bg_quest_icon_purple)
                    }
                    1 -> {
                        tvQuestEmoji.text = "📚"
                        tvQuestEmoji.setBackgroundResource(R.drawable.bg_quest_icon_yellow)
                    }
                    else -> {
                        tvQuestEmoji.text = "💡"
                        tvQuestEmoji.setBackgroundResource(R.drawable.bg_quest_icon_green)
                    }
                }
                
                // 2. 상태에 따른 박스 테두리 및 액션 영역 설정
                if (quest.isCompleted) {
                    root.setBackgroundResource(R.drawable.bg_quest_item_inactive) // 완료된 건 어두운 테두리
                    ivCompleted.visibility = View.VISIBLE
                    ivCompleted.setImageResource(R.drawable.bg_quest_completed_check) // 시안과 동일한 체크 박스
                    tvStartBtn.visibility = View.GONE
                    
                    // 완료된 카드는 클릭 비활성화
                    root.setOnClickListener(null)
                    root.setOnTouchListener(null)
                } else {
                    root.setBackgroundResource(R.drawable.bg_quest_item_active) // 진행 중인 건 선명한 파란 테두리
                    ivCompleted.visibility = View.GONE
                    tvStartBtn.visibility = if (quest.canStart) View.VISIBLE else View.GONE
                    
                    // [수정] 카드 전체가 아닌 '시작' 버튼에만 클릭 리스너와 애니메이션 적용
                    root.setOnClickListener(null) 
                    root.setOnTouchListener(null)

                    tvStartBtn.setOnScaleTouchListener()
                    tvStartBtn.setOnClickListener {
                        if (quest.canStart) {
                            // 탭 전환을 통해 이동
                            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                            bottomNav.selectedItemId = R.id.learningFragment
                        }
                    }
                }
            }
            binding.layoutQuestList.addView(itemBinding.root)
        }
    }
}
