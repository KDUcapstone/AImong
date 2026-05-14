package com.kduniv.aimong.feature.dev.mock

import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.presentation.HomeLayoutBinder
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.home.presentation.MissionDifficultyPicker
import com.kduniv.aimong.feature.home.presentation.QuestListBottomSheet
import com.kduniv.aimong.feature.home.presentation.StreakCalendarBottomSheet

/** [HomeFragment]와 동일 레이아웃 — [MockUiSamples] 고정 데이터. */
class MockHomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val sampleState get() = MockUiSamples.homeUiState()
    private var missionDifficultyPicker: MissionDifficultyPicker? = null

    override fun initView() {
        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            onOpenDifficultyPicker = { title, nav, anchor ->
                missionDifficultyPicker?.dismissImmediate()
                val picker = MissionDifficultyPicker(binding, layoutInflater)
                missionDifficultyPicker = picker
                picker.show(title, nav, anchor) { picked ->
                    findNavController().navigate(
                        MockHomeFragmentDirections.actionHomeFragmentToQuizFragment(
                            picked.entrySetId,
                            picked.missionId,
                            picked.starLevel,
                        ),
                    )
                    missionDifficultyPicker = null
                }
            },
            onShowMissionHint = { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() },
        )
        binding.fabChildQuest.setOnClickListener {
            QuestListBottomSheet.newInstance(canStartMission = true)
                .show(parentFragmentManager, "quest_list")
        }
        binding.cardFloatPet.setOnClickListener { showPetStatsSheet() }

        binding.layoutChipTicket.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.gachaFragment
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
