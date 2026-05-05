package com.kduniv.aimong.feature.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.DialogQuestListBinding
import com.kduniv.aimong.feature.home.presentation.quest.QuestListViewModel
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetEffect
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPeriod
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction
import com.kduniv.aimong.feature.home.presentation.quest.QuestSheetRow
import dagger.hilt.android.AndroidEntryPoint
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

        binding.tabQuestPeriod.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectPeriod(QuestSheetPeriod.DAILY)
                    1 -> viewModel.selectPeriod(QuestSheetPeriod.WEEKLY)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectPeriod(QuestSheetPeriod.DAILY)
                    1 -> viewModel.selectPeriod(QuestSheetPeriod.WEEKLY)
                }
            }
        })

        if (savedInstanceState == null) {
            binding.tabQuestPeriod.getTabAt(0)?.select()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rows.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.loading.collect { loading ->
                        binding.pbQuestLoading.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is QuestSheetEffect.ShowToast ->
                                Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                            is QuestSheetEffect.Snackbar ->
                                Snackbar.make(binding.root, effect.message, Snackbar.LENGTH_LONG).show()
                            is QuestSheetEffect.TicketsPatched ->
                                homeViewModel.applyRemainingTickets(effect.normal, effect.rare, effect.epic)
                        }
                    }
                }
            }
        }
    }

    private fun onQuestRowClicked(row: QuestSheetRow) {
        when (row.primaryAction) {
            QuestSheetPrimaryAction.CLAIM ->
                viewModel.onClaim(row.questType, row.period)
            QuestSheetPrimaryAction.GO_LEARN -> {
                dismiss()
                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                    .selectedItemId = R.id.learningFragment
            }
            QuestSheetPrimaryAction.GO_CHAT -> {
                dismiss()
                val navHost = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val nav = navHost.navController
                if (nav.currentDestination?.id != R.id.chatFragment) {
                    nav.navigate(R.id.chatFragment)
                }
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CAN_START_MISSION = "quest_can_start_mission"

        fun newInstance(canStartMission: Boolean = true): QuestListBottomSheet {
            return QuestListBottomSheet().apply {
                arguments = bundleOf(ARG_CAN_START_MISSION to canStartMission)
            }
        }
    }
}
