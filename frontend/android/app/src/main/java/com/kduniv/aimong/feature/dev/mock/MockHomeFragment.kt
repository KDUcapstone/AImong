package com.kduniv.aimong.feature.dev.mock

import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.presentation.EnergyBottomSheet
import com.kduniv.aimong.feature.home.presentation.GearBottomSheet
import com.kduniv.aimong.feature.home.presentation.HomeLayoutBinder
import com.kduniv.aimong.feature.home.presentation.DifficultyUnlockMode
import com.kduniv.aimong.feature.home.presentation.MissionDifficultyPicker
import com.kduniv.aimong.feature.home.presentation.QuestListBottomSheet
import com.kduniv.aimong.feature.home.presentation.StreakCalendarBottomSheet
import com.kduniv.aimong.feature.quiz.presentation.QuizFragment
import dagger.hilt.android.AndroidEntryPoint

/** [HomeFragment]와 동일 레이아웃 — [MockUiSamples] 고정 데이터. */
@AndroidEntryPoint
class MockHomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val sampleState get() = MockUiSamples.homeUiState()
    private var missionDifficultyPicker: MissionDifficultyPicker? = null

    override fun initView() {
        childFragmentManager.setFragmentResultListener(
            EnergyBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(EnergyBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post {
                    homeLayoutBinder.bind(MockUiSamples.homeUiState())
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
                    homeLayoutBinder.bind(MockUiSamples.homeUiState())
                }
            }
        }

        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            onOpenDifficultyPicker = { title, nav, anchor, unlockMode ->
                val st = sampleState
                if (!st.canOpenMissionPicker(unlockMode)) {
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
                    missionDifficultyPicker?.dismissImmediate()
                    val picker = MissionDifficultyPicker(binding, layoutInflater)
                    missionDifficultyPicker = picker
                    picker.show(title, nav, emptyList(), unlockMode, anchor) { picked, _ ->
                        findNavController().navigate(
                            MockHomeFragmentDirections.actionHomeFragmentToQuizFragment(
                                picked.entrySetId,
                                picked.missionId,
                                picked.starLevel,
                            ),
                        )
                        missionDifficultyPicker = null
                    }
                }
            },
            onNavigateToQuiz = { nav, _ ->
                findNavController().navigate(
                    MockHomeFragmentDirections.actionHomeFragmentToQuizFragment(
                        nav.entrySetId,
                        nav.missionId,
                        nav.starLevel,
                    ),
                )
            },
            onEnergyInsufficient = {
                Snackbar.make(
                    binding.root,
                    getString(R.string.home_energy_insufficient_toast),
                    Snackbar.LENGTH_SHORT,
                ).show()
            },
            onShowMissionHint = { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() },
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
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.gachaFragment
        }
        parentFragmentManager.setFragmentResultListener(
            StreakCalendarBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(StreakCalendarBottomSheet.EXTRA_REFRESH_HOME, false)) {
                binding.root.post { homeLayoutBinder.bind(MockUiSamples.homeUiState()) }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            QuizFragment.REQUEST_QUIZ_FINISHED,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getBoolean(QuizFragment.EXTRA_REFRESH_HOME, false)) {
                binding.root.post { homeLayoutBinder.bind(MockUiSamples.homeUiState()) }
            }
        }
        binding.layoutChipStreak.setOnClickListener {
            StreakCalendarBottomSheet.newInstance(sampleState.streakDays)
                .show(parentFragmentManager, "streak_calendar")
        }

        binding.root.post { homeLayoutBinder.bind(sampleState) }
    }

    override fun onDestroyView() {
        missionDifficultyPicker?.dismissImmediate()
        missionDifficultyPicker = null
        super.onDestroyView()
    }

    private fun showPetStatsSheet() {
        val s = sampleState
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottomsheet_pet_stats, null, false)
        v.findViewById<TextView>(R.id.tv_pet_emoji).text = when (s.petStage) {
            "EGG" -> "🥚"
            "GROWTH" -> "🐣"
            else -> "✨"
        }
        v.findViewById<TextView>(R.id.tv_pet_name).text =
            s.petName.ifBlank { getString(R.string.home_pet_name_default) }
        v.findViewById<TextView>(R.id.tv_pet_level).text =
            getString(R.string.home_pet_level_fmt, s.petLevel)
        val maxXp = s.petMaxXp.coerceAtLeast(1)
        val pct = ((s.petXp.toFloat() / maxXp) * 100f).toInt().coerceIn(0, 100)
        v.findViewById<ProgressBar>(R.id.progress_pet_xp).progress = pct
        v.findViewById<TextView>(R.id.tv_pet_xp_label).text =
            getString(R.string.home_pet_xp_fmt, s.petXp, s.petMaxXp)
        dialog.setContentView(v)
        dialog.show()
    }

    override fun initObserver() {}
}
