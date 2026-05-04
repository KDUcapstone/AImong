package com.kduniv.aimong.feature.home.presentation

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.databinding.ItemHomeQuestBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeCompletedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeLockedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeReviewBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeStartBinding
import com.kduniv.aimong.databinding.ViewHomePetStandaloneBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()
    private var pathSideIndex = 0

    override fun onResume() {
        super.onResume()
        viewModel.onHomeResumed()
    }

    override fun initView() {
        binding.btnGacha.apply {
            setOnClickListener { openGacha() }
            setOnScaleTouchListener()
        }
        binding.tvChipTicket.setOnClickListener { openGacha() }
        binding.tvChipStreak.setOnClickListener { openStreakSheet() }
    }

    private fun openGacha() {
        findNavController().navigate(R.id.action_homeFragment_to_gachaFragment)
    }

    private fun openStreakSheet() {
        val streak = viewModel.uiState.value.streakDays
        StreakCalendarBottomSheet.newInstance(streak).show(parentFragmentManager, "streak_calendar")
    }

    private fun navigateToQuiz(missionId: String) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToQuizFragment(missionId)
        )
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.root.post {
                        updateUi(state)
                        state.errorMessage?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                            viewModel.consumeError()
                        }
                        state.subtleNotice?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.consumeSubtleNotice()
                        }
                    }
                }
            }
        }
    }

    private fun updateUi(state: HomeUiState) {
        with(binding) {
            tvChipHeart.text = "❤️ ${state.heartCount}"
            tvChipXp.text = "⚡ ${state.topStatusXp}"
            tvChipTicket.text = "🎟 ${state.topTicketCount}"
            tvChipStreak.text = "🔥 ${state.streakDays}일"

            tvHomeSubtitle.text = "Lv.${state.userLevel} ${viewModel.getProfileLabel(state.profileType)}"

            if (state.petMessage.isNotBlank()) {
                tvPetMessage.isVisible = true
                tvPetMessage.text = state.petMessage
            } else {
                tvPetMessage.isVisible = false
            }

            renderMissionPath(state)

            tvPetXpLabel.text = "${state.petXp} / ${state.petMaxXp} EXP"
            pbXp.max = state.petMaxXp
            pbXp.progress = state.petXp

            tvQuestProgressBadge.text = "${state.todayQuestProgress} 완료"
            setupQuestList(state.quests)

            tvGachaTitle.text = getString(R.string.home_gacha_ticket_title, state.normalTickets)
            tvGachaDesc.text = state.gachaDescription.ifBlank {
                getString(R.string.home_gacha_until_loaded)
            }
        }
    }

    private fun renderMissionPath(state: HomeUiState) {
        binding.layoutMissionPath.removeAllViews()
        pathSideIndex = 0
        val inflater = layoutInflater
        val items = state.pathItems
        val hasStart = items.any { it is HomePathItem.TodayStart }
        val petLabel = state.petName.ifBlank { getString(R.string.home_pet_name_default) }
        val petNameText = "$petLabel Lv.${state.petLevel}"

        if (!hasStart) {
            val stand = ViewHomePetStandaloneBinding.inflate(inflater, binding.layoutMissionPath, true)
            stand.tvPetStandaloneName.text = petNameText
            if (!stand.lottiePetStandalone.isAnimating) stand.lottiePetStandalone.playAnimation()
        }

        for (item in items) {
            when (item) {
                is HomePathItem.Completed -> {
                    val node = ViewHomePathNodeCompletedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    node.tvOrder.text = item.order.toString()
                    node.tvMissionTitle.text = item.title
                    addSideNode(node.root)
                }
                is HomePathItem.TodayStart -> {
                    val block = ViewHomePathNodeStartBinding.inflate(inflater, binding.layoutMissionPath, false)
                    block.tvMissionTitle.text = item.missionTitle
                    block.btnStartMission.isEnabled = item.enabled
                    block.btnStartMission.alpha = if (item.enabled) 1f else 0.45f
                    block.tvPetNameLevel.text = petNameText
                    block.btnStartMission.setOnClickListener {
                        if (item.enabled) navigateToQuiz(item.missionId)
                    }
                    if (!block.lottiePet.isAnimating) block.lottiePet.playAnimation()
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    binding.layoutMissionPath.addView(block.root, lp)
                }
                is HomePathItem.Review -> {
                    val node = ViewHomePathNodeReviewBinding.inflate(inflater, binding.layoutMissionPath, false)
                    node.tvReviewSub.text = item.subtitle
                    node.root.setOnClickListener { navigateToQuiz(item.missionId) }
                    addSideNode(node.root)
                }
                is HomePathItem.Locked -> {
                    val node = ViewHomePathNodeLockedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    node.tvLockedHint.text = item.hint
                    addSideNode(node.root)
                }
            }
        }
    }

    private fun addSideNode(nodeView: View) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 6, 0, 6)
        }
        val left = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val right = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(left)
        row.addView(right)

        val target = if (pathSideIndex % 2 == 0) left else right
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (pathSideIndex % 2 == 0) Gravity.END else Gravity.START
        }
        target.addView(nodeView, lp)
        pathSideIndex++
        binding.layoutMissionPath.addView(row)
    }

    private fun setupQuestList(quests: List<QuestItemUiState>) {
        binding.layoutQuestList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        quests.forEachIndexed { index, quest ->
            val itemBinding = ItemHomeQuestBinding.inflate(inflater, binding.layoutQuestList, false)
            with(itemBinding) {
                tvQuestTitle.text = quest.title
                tvQuestReward.text = quest.rewardSummary

                when (index % 3) {
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

                if (quest.isCompleted) {
                    root.setBackgroundResource(R.drawable.bg_quest_item_inactive)
                    ivCompleted.visibility = View.VISIBLE
                    ivCompleted.setImageResource(R.drawable.bg_quest_completed_check)
                    tvStartBtn.visibility = View.GONE
                    root.setOnClickListener(null)
                    root.setOnTouchListener(null)
                } else {
                    root.setBackgroundResource(R.drawable.bg_quest_item_active)
                    ivCompleted.visibility = View.GONE
                    tvStartBtn.visibility = if (quest.canStart) View.VISIBLE else View.GONE
                    root.setOnClickListener(null)
                    root.setOnTouchListener(null)

                    tvStartBtn.setOnScaleTouchListener()
                    tvStartBtn.setOnClickListener {
                        if (quest.canStart) {
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
