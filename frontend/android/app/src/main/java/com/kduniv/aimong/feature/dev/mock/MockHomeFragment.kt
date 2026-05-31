package com.kduniv.aimong.feature.dev.mock

import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.onChildBottomNavTap
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.presentation.EnergyBottomSheet
import com.kduniv.aimong.feature.home.presentation.GearBottomSheet
import com.kduniv.aimong.feature.home.presentation.HomeLayoutBinder
import com.kduniv.aimong.feature.home.presentation.PetStatsSheetPresenter
import com.kduniv.aimong.feature.home.presentation.DifficultyUnlockMode
import com.kduniv.aimong.feature.home.presentation.MissionDifficultyPicker
import com.kduniv.aimong.feature.home.presentation.difficultyPickerMissionKey
import com.kduniv.aimong.feature.home.presentation.QuestListBottomSheet
import com.kduniv.aimong.feature.home.presentation.StreakCalendarBottomSheet
import com.kduniv.aimong.feature.gacha.PetArtAssets
import com.kduniv.aimong.feature.quiz.presentation.QuizFragment
/** [HomeFragment]와 동일 레이아웃 — [MockUiSamples] 고정 데이터. */
@AndroidEntryPoint
class MockHomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    @Inject lateinit var homeRefreshBus: ChildHomeRefreshBus

    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val sampleState get() = MockUiSamples.homeUiState()
    private val missionDifficultyPicker: MissionDifficultyPicker by lazy {
        MissionDifficultyPicker(binding, layoutInflater)
    }

    override fun initView() {
        childFragmentManager.setFragmentResultListener(
            EnergyBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(EnergyBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post {
                    bindHome()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.mock_home_energy_refreshed),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
        childFragmentManager.setFragmentResultListener(
            GearBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(GearBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post {
                    bindHome()
                }
            }
        }

        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            onMissionPathWillRebuild = { missionDifficultyPicker.dismissImmediate() },
            onOpenDifficultyPicker = { title, nav, anchor, unlockMode ->
                if (!sampleState.hasEnoughEnergyForMissionStart()) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.home_energy_insufficient_toast),
                        Snackbar.LENGTH_LONG,
                    )
                        .setAction(getString(R.string.home_go_energy_charge)) {
                            EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
                        }
                        .show()
                } else {
                    val missionKey = nav.difficultyPickerMissionKey()
                    if (missionDifficultyPicker.isShowingForMission(missionKey)) {
                        missionDifficultyPicker.dismissAnimated()
                    } else {
                        missionDifficultyPicker.dismissImmediate()
                        missionDifficultyPicker.show(
                            missionTitle = title,
                            base = nav,
                            starLevels = emptyList(),
                            unlockMode = unlockMode,
                            anchorRow = anchor,
                            missionKey = missionKey,
                            onPicked = { picked, _ ->
                                findNavController().navigate(
                                    R.id.quizFragment,
                                    androidx.core.os.bundleOf(
                                        "entrySetId" to picked.entrySetId,
                                        "missionId" to picked.missionId,
                                        "starLevel" to picked.starLevel,
                                    ),
                                )
                            },
                        )
                    }
                }
            },
            onNavigateToQuiz = { nav, _ ->
                findNavController().navigate(
                    R.id.quizFragment,
                    androidx.core.os.bundleOf(
                        "entrySetId" to nav.entrySetId,
                        "missionId" to nav.missionId,
                        "starLevel" to nav.starLevel,
                    ),
                )
            },
            onShowMissionHint = { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() },
            onStageRewardChestClick = { reward ->
                com.kduniv.aimong.feature.home.presentation.StageRewardDialog.show(this, reward)
            },
        )
        binding.layoutChipEnergy.setOnClickListener {
            EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
        }
        binding.layoutChipGear.setOnClickListener {
            GearBottomSheet.newInstance().show(childFragmentManager, "gear_sheet")
        }
        binding.fabChildQuest.setOnClickListener {
            QuestListBottomSheet.newInstance(canStartMission = true)
                .show(parentFragmentManager, "quest_list")
        }
        binding.cardFloatPet.setOnClickListener { showPetStatsSheet() }

        binding.layoutChipTicket.setOnClickListener {
            findNavController().onChildBottomNavTap(R.id.gachaFragment)
        }
        parentFragmentManager.setFragmentResultListener(
            StreakCalendarBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(StreakCalendarBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post { bindHome() }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            QuizFragment.REQUEST_QUIZ_FINISHED,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getBoolean(QuizFragment.EXTRA_REFRESH_HOME, false)) {
                binding.root.post { bindHome() }
            }
        }
        binding.layoutChipStreak.setOnClickListener {
            StreakCalendarBottomSheet.newInstance(
                fallbackStreakDaysFromHome = sampleState.streakDays,
                petType = sampleState.equippedPetType,
                petStage = sampleState.petStage,
                petGrade = sampleState.equippedPetGrade,
            ).show(parentFragmentManager, "streak_calendar")
        }

        binding.root.post { bindHome() }
    }

    override fun onResume() {
        super.onResume()
        if (::homeLayoutBinder.isInitialized) {
            bindHome()
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeRefreshBus.events.collect { trigger ->
                    if (trigger is HomeRefreshTrigger.Full && ::homeLayoutBinder.isInitialized) {
                        bindHome()
                    }
                }
            }
        }
    }

    private fun bindHome() {
        missionDifficultyPicker.dismissImmediate()
        homeLayoutBinder.bind(MockUiSamples.homeUiState())
    }

    override fun onDestroyView() {
        if (::homeLayoutBinder.isInitialized) {
            missionDifficultyPicker.dismissImmediate()
        }
        super.onDestroyView()
    }

    private fun showPetStatsSheet() {
        PetStatsSheetPresenter.show(this, sampleState)
    }
}
