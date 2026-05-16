package com.kduniv.aimong.feature.home.presentation

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private var missionDifficultyPicker: MissionDifficultyPicker? = null

    override fun onResume() {
        super.onResume()
        viewModel.onHomeResumed()
    }

    override fun initView() {
        childFragmentManager.setFragmentResultListener(
            EnergyBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(EnergyBottomSheet.EXTRA_REFRESH_HOME, false)) {
                viewModel.onHomeResumed()
            }
        }
        childFragmentManager.setFragmentResultListener(
            GearBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(GearBottomSheet.EXTRA_REFRESH_HOME, false)) {
                viewModel.onHomeResumed()
            }
        }
        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            onOpenDifficultyPicker = { title, nav, anchor ->
                val st = viewModel.uiState.value
                if (!st.canAttemptMissionStart()) {
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
                    val starLevels = viewModel.missionStarLevels(nav.missionId)
                    picker.show(title, nav, starLevels, anchor) { picked ->
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeFragmentToQuizFragment(
                                picked.entrySetId,
                                picked.missionId,
                                picked.starLevel,
                            ),
                        )
                        missionDifficultyPicker = null
                    }
                }
            },
            onShowMissionHint = { showMissionHint(it) },
        )
        binding.layoutChipEnergy.setOnClickListener {
            EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
        }
        binding.layoutChipGear.setOnClickListener {
            GearBottomSheet.newInstance().show(childFragmentManager, "gear_sheet")
        }
        binding.fabChildQuest.setOnClickListener { openQuestList() }
        binding.cardFloatPet.setOnClickListener { showPetStatsSheet() }
        binding.layoutChipTicket.setOnClickListener { openGacha() }
        parentFragmentManager.setFragmentResultListener(
            StreakCalendarBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(StreakCalendarBottomSheet.EXTRA_REFRESH_HOME, false)) {
                viewModel.onHomeResumed()
            }
        }
        binding.layoutChipStreak.setOnClickListener { openStreakSheet() }
        parentFragmentManager.setFragmentResultListener(
            QuestListBottomSheet.REQUEST_OPEN_MISSION_LEARN,
            viewLifecycleOwner,
        ) { _, _ ->
            binding.root.post { openMissionLearnFromQuest() }
        }
    }

    private fun openMissionLearnFromQuest() {
        viewLifecycleOwner.lifecycleScope.launch {
            val state = viewModel.uiState.first { !it.isLoading }
            openMissionLearnFromQuest(state)
        }
    }

    private fun openMissionLearnFromQuest(state: HomeUiState) {
        if (!state.canAttemptMissionStart()) {
            Snackbar.make(
                binding.root,
                getString(R.string.home_energy_insufficient_toast),
                Snackbar.LENGTH_LONG,
            )
                .setAction(getString(R.string.home_go_energy_charge)) {
                    EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
                }
                .show()
            return
        }
        val nav = viewModel.resolveQuestLearnQuizNav()
        if (nav == null || !nav.canNavigate()) {
            showMissionHint(getString(R.string.mission_no_playable_star_level))
            return
        }
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToQuizFragment(
                nav.entrySetId,
                nav.missionId,
                nav.starLevel,
            ),
        )
    }

    override fun onDestroyView() {
        missionDifficultyPicker?.dismissImmediate()
        missionDifficultyPicker = null
        super.onDestroyView()
    }

    private fun openGacha() {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.gachaFragment
    }

    private fun openStreakSheet() {
        val streak = viewModel.uiState.value.streakDays
        StreakCalendarBottomSheet.newInstance(streak).show(parentFragmentManager, "streak_calendar")
    }

    private fun showMissionHint(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showPetStatsSheet() {
        val s = viewModel.uiState.value
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

    private fun openQuestList() {
        QuestListBottomSheet.newInstance(canStartMission = viewModel.uiState.value.canStartMission)
            .show(parentFragmentManager, "quest_list")
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.root.post {
                        missionDifficultyPicker?.dismissImmediate()
                        missionDifficultyPicker = null
                        homeLayoutBinder.bind(state)
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
}
