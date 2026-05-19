package com.kduniv.aimong.feature.home.presentation.my

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentChildMyProfileBinding
import com.kduniv.aimong.databinding.ItemChildMyProfileMenuRowBinding
import com.kduniv.aimong.feature.auth.domain.LogoutChildUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
open class ChildMyProfileFragment :
    BaseFragment<FragmentChildMyProfileBinding>(FragmentChildMyProfileBinding::inflate) {

    private val viewModel: ChildMyProfileViewModel by viewModels()

    @Inject
    lateinit var logoutChildUseCase: LogoutChildUseCase

    private val badgeAdapter = ChildMyProfileBadgeAdapter()
    private var profileLoadRequested = false

    override fun shouldApplySystemBarInsets(): Boolean = false

    override fun initView() {
        binding.rvBadges.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = badgeAdapter
            itemAnimator = null
        }

        bindMenuRow(
            binding.rowNotifications,
            iconRes = R.drawable.ic_notifications,
            iconBgRes = R.drawable.bg_child_my_icon_circle_purple,
            label = getString(R.string.notification_settings_title)
        ) {
            findNavController().navigate(
                R.id.action_myProfileFragment_to_notificationSettingsFragment
            )
        }

        bindMenuRow(
            binding.rowLogout,
            iconRes = R.drawable.ic_exit_logout,
            iconBgRes = R.drawable.bg_child_my_icon_circle_orange,
            label = getString(R.string.child_logout)
        ) {
            confirmLogout()
        }

        bindStatTiles()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> bindState(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncChildBottomNavSelection()
        if (!profileLoadRequested) {
            profileLoadRequested = true
            viewModel.onScreenVisible()
        }
    }

    /** 화면은 MY인데 하단바가 이전 탭(수집 등)으로 남는 경우 보정 */
    private fun syncChildBottomNavSelection() {
        (activity as? MainActivity)?.syncChildBottomNavForCurrentDestination()
    }

    private fun bindStatTiles() {
        binding.statMissions.tvStatLabel.setText(R.string.child_my_stat_missions)
        binding.statXp.tvStatLabel.setText(R.string.child_my_stat_xp)
        binding.statPets.tvStatLabel.setText(R.string.child_my_stat_pets)
        binding.statStreak.tvStatLabel.setText(R.string.child_my_stat_streak)
    }

    private fun bindState(state: ChildMyProfileUiState) {
        val rootBinding = _binding ?: return
        rootBinding.pbLoading.isVisible = state.isLoading && state.nickname.isBlank()
        rootBinding.tvNickname.text = state.nickname
        rootBinding.tvProfileType.text = state.profileSubtitle

        rootBinding.statMissions.tvStatValue.text = state.completedMissionCount.toString()
        rootBinding.statXp.tvStatValue.text = state.totalXp.toString()
        rootBinding.statPets.tvStatValue.text = state.petCount.toString()
        rootBinding.statStreak.tvStatValue.text = getString(
            R.string.child_my_streak_days_fmt,
            state.streakDays
        )

        val unlocked = state.badges.count { it.isUnlocked }
        val total = state.badges.size
        if (total > 0) {
            rootBinding.tvBadgesSubtitle.isVisible = true
            rootBinding.tvBadgesSubtitle.text = getString(
                R.string.child_my_badges_subtitle_fmt,
                unlocked,
                total
            )
            rootBinding.rvBadges.isVisible = true
            rootBinding.tvBadgesEmpty.isVisible = false
            val badges = state.badges.toList()
            rootBinding.rvBadges.post { badgeAdapter.submitList(badges) }
        } else if (!state.isLoading) {
            rootBinding.tvBadgesSubtitle.isVisible = false
            rootBinding.rvBadges.isVisible = false
            rootBinding.tvBadgesEmpty.isVisible = true
            rootBinding.rvBadges.post { badgeAdapter.submitList(emptyList()) }
        }
    }

    private fun bindMenuRow(
        rowBinding: ItemChildMyProfileMenuRowBinding,
        iconRes: Int,
        iconBgRes: Int,
        label: String,
        onClick: () -> Unit
    ) {
        rowBinding.frameIcon.setBackgroundResource(iconBgRes)
        rowBinding.ivIcon.setImageResource(iconRes)
        rowBinding.tvLabel.text = label
        rowBinding.root.setOnClickListener { onClick() }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.child_logout_confirm)
            .setPositiveButton(R.string.child_logout) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    logoutChildUseCase()
                    startActivity(
                        Intent(requireContext(), MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra(MainActivity.EXTRA_IS_RESTART, true)
                        }
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
