package com.kduniv.aimong.feature.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.kduniv.aimong.R
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.navigateToChildTopLevel
import com.kduniv.aimong.databinding.DialogQuestListBinding
import com.kduniv.aimong.feature.home.presentation.quest.QuestListViewModel
import com.kduniv.aimong.feature.home.presentation.quest.QuestRewardCelebrationDialog
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetEffect
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPeriod
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetRow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuestListBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogQuestListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestListViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var adapter: QuestListAdapter

    override fun getTheme(): Int = R.style.AimongBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val canStart = arguments?.getBoolean(ARG_CAN_START_MISSION) ?: true
        viewModel.setCanStartMission(canStart)

        adapter = QuestListAdapter { row -> onQuestRowClicked(row) }
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.adapter = adapter

        binding.tabQuestPeriod.addTab(
            binding.tabQuestPeriod.newTab().setText(getString(R.string.quest_tab_daily))
        )
        binding.tabQuestPeriod.addTab(
            binding.tabQuestPeriod.newTab().setText(getString(R.string.quest_tab_weekly))
        )
        binding.tabQuestPeriod.addTab(
            binding.tabQuestPeriod.newTab().setText(getString(R.string.quest_tab_parent))
        )

        binding.tabQuestPeriod.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectPeriod(QuestSheetPeriod.DAILY)
                    1 -> viewModel.selectPeriod(QuestSheetPeriod.WEEKLY)
                    2 -> viewModel.selectPeriod(QuestSheetPeriod.PARENT)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectPeriod(QuestSheetPeriod.DAILY)
                    1 -> viewModel.selectPeriod(QuestSheetPeriod.WEEKLY)
                    2 -> viewModel.selectPeriod(QuestSheetPeriod.PARENT)
                }
            }
        })

        if (savedInstanceState == null) {
            binding.tabQuestPeriod.getTabAt(0)?.select()
        }

        // 이전에 이미 확인한 경우에만 탭 배지 숨김 — 첫 열기에서는 API 직후 빨간 점 표시
        viewModel.setTabBadgesSuppressed(homeViewModel.uiState.value.shouldSuppressQuestSheetTabBadges())
        viewModel.onSheetOpened()
        homeViewModel.acknowledgeQuestNotifications()

        binding.btnQuestRetry.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rows.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.loading.collect { loading ->
                        binding.layoutQuestLoadingOverlay.isVisible = loading
                        adapter.setSheetLoading(loading)
                        setQuestTabsEnabled(binding.tabQuestPeriod, !loading)
                    }
                }
                launch {
                    combine(viewModel.loadError, viewModel.loading) { err, loading ->
                        err to loading
                    }.collect { (err, loading) ->
                        val show = err != null && !loading
                        binding.layoutQuestError.isVisible = show
                        if (show) binding.tvQuestError.text = err
                    }
                }
                launch {
                    combine(viewModel.emptyMessage, viewModel.loading, viewModel.loadError) { empty, loading, err ->
                        Triple(empty, loading, err)
                    }.collect { (empty, loading, err) ->
                        val show = empty != null && !loading && err == null
                        binding.tvQuestEmpty.isVisible = show
                        binding.rvQuests.isVisible = !show
                        if (show) binding.tvQuestEmpty.text = empty
                    }
                }
                launch {
                    viewModel.dailyTabBadgeCount.collect { count ->
                        bindQuestTabBadge(binding.tabQuestPeriod.getTabAt(0), count)
                    }
                }
                launch {
                    viewModel.weeklyTabBadgeCount.collect { count ->
                        bindQuestTabBadge(binding.tabQuestPeriod.getTabAt(1), count)
                    }
                }
                launch {
                    viewModel.parentTabBadgeCount.collect { count ->
                        bindQuestTabBadge(binding.tabQuestPeriod.getTabAt(2), count)
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is QuestSheetEffect.ShowRewardCelebration ->
                                QuestRewardCelebrationDialog.show(this@QuestListBottomSheet, effect.ui)
                            is QuestSheetEffect.Snackbar ->
                                Snackbar.make(binding.root, effect.message, Snackbar.LENGTH_LONG).show()
                            is QuestSheetEffect.TicketsPatched ->
                                homeViewModel.applyRemainingTickets(effect.normal)
                        }
                    }
                }
            }
        }
    }

    private fun bindQuestTabBadge(tab: TabLayout.Tab?, count: Int) {
        if (tab == null) return
        val badge = tab.orCreateBadge
        if (count <= 0) {
            badge.isVisible = false
            badge.clearNumber()
        } else {
            badge.isVisible = true
            badge.clearNumber()
        }
    }

    private fun setQuestTabsEnabled(tabLayout: TabLayout, enabled: Boolean) {
        tabLayout.isEnabled = enabled
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.isClickable = enabled
        }
    }

    private fun onQuestRowClicked(row: QuestSheetRow) {
        when (row.primaryAction) {
            QuestSheetPrimaryAction.COMPLETE_CUSTOM ->
                viewModel.onCompleteCustomQuest(row.questType)
            QuestSheetPrimaryAction.CLAIM ->
                viewModel.onClaim(row.questType, row.period, row.title)
            QuestSheetPrimaryAction.GO_LEARN -> {
                parentFragmentManager.setFragmentResult(
                    REQUEST_OPEN_MISSION_LEARN,
                    bundleOf(),
                )
                dismiss()
            }
            QuestSheetPrimaryAction.GO_CHAT -> {
                dismiss()
                navigateChildTopLevel(R.id.chatFragment)
            }
            else -> Unit
        }
    }

    private fun navigateChildTopLevel(@androidx.annotation.IdRes destinationId: Int) {
        val navHost = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val nav = navHost.navController
        if (nav.currentDestination?.id != destinationId) {
            nav.navigateToChildTopLevel(destinationId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_OPEN_MISSION_LEARN = "quest_open_mission_learn"

        private const val ARG_CAN_START_MISSION = "quest_can_start_mission"

        fun newInstance(canStartMission: Boolean = true): QuestListBottomSheet {
            return QuestListBottomSheet().apply {
                arguments = bundleOf(ARG_CAN_START_MISSION to canStartMission)
            }
        }
    }
}
