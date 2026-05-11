package com.kduniv.aimong.feature.dev.mock

import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.presentation.HomeLayoutBinder
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.home.presentation.QuestListBottomSheet
import com.kduniv.aimong.feature.home.presentation.StreakCalendarBottomSheet

/** [HomeFragment]와 동일 레이아웃 — [MockUiSamples] 고정 데이터. */
class MockHomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val sampleState get() = MockUiSamples.homeUiState()

    override fun initView() {
        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            getProfileLabel = { MockUiSamples.profileLabel(it) },
            petNameDefault = getString(R.string.home_pet_name_default),
            onNavigateQuiz = { nav: HomeQuizNavigation ->
                findNavController().navigate(
                    MockHomeFragmentDirections.actionHomeFragmentToQuizFragment(
                        nav.entrySetId,
                        nav.missionId,
                        nav.starLevel
                    )
                )
            },
            onOpenQuest = {
                QuestListBottomSheet.newInstance(canStartMission = true)
                    .show(parentFragmentManager, "quest_list")
            }
        )

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
