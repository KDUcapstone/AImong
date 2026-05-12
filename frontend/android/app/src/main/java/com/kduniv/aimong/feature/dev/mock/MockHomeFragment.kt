package com.kduniv.aimong.feature.dev.mock

import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.presentation.EnergyBottomSheet
import com.kduniv.aimong.feature.home.presentation.HomeLayoutBinder
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.home.presentation.QuestListBottomSheet
import com.kduniv.aimong.feature.home.presentation.StreakCalendarBottomSheet
import dagger.hilt.android.AndroidEntryPoint

/** [HomeFragment]와 동일 레이아웃 — [MockUiSamples] 고정 데이터. */
@AndroidEntryPoint
class MockHomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val sampleState get() = MockUiSamples.homeUiState()

    override fun initView() {
        childFragmentManager.setFragmentResultListener(
            EnergyBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(EnergyBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post {
                    homeLayoutBinder.bind(sampleState)
                    Snackbar.make(
                        binding.root,
                        getString(R.string.mock_home_energy_refreshed),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            getProfileLabel = { MockUiSamples.profileLabel(it) },
            petNameDefault = getString(R.string.home_pet_name_default),
            onNavigateQuiz = { nav: HomeQuizNavigation ->
                val st = sampleState
                if (!st.canStartMission || st.energyCurrent < 1) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.home_energy_insufficient_toast),
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(getString(R.string.home_go_energy_charge)) {
                            EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
                        }
                        .show()
                } else {
                    findNavController().navigate(
                        MockHomeFragmentDirections.actionHomeFragmentToQuizFragment(
                            nav.entrySetId,
                            nav.missionId,
                            nav.starLevel
                        )
                    )
                }
            },
            onOpenQuest = {
                QuestListBottomSheet.newInstance(canStartMission = true)
                    .show(parentFragmentManager, "quest_list")
            }
        )

        binding.tvChipEnergy.setOnClickListener {
            EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
        }
        binding.tvChipTicket.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.gachaFragment
        }
        binding.tvChipStreak.setOnClickListener {
            StreakCalendarBottomSheet.newInstance(sampleState.streakDays)
                .show(parentFragmentManager, "streak_calendar")
        }

        binding.root.post { homeLayoutBinder.bind(sampleState) }
    }

    override fun initObserver() {}
}
