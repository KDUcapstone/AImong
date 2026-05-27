package com.kduniv.aimong.feature.home.presentation

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.onChildBottomNavTap
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.mission.domain.model.normalizeToThreeLevels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val missionDifficultyPicker: MissionDifficultyPicker by lazy {
        MissionDifficultyPicker(binding, layoutInflater)
    }
    private var missionPickerStarLevelsJob: Job? = null
    private var lastHomePathStructureKey: String? = null

    override fun onResume() {
        super.onResume()
        viewModel.onHomeResumed()
        viewModel.pendingAimongCelebration.value?.let { pending ->
            binding.root.post {
                if (!isAdded) return@post
                AimongCelebrationDialog.show(this, pending)
                viewModel.consumeAimongCelebration()
            }
        }
    }

    override fun initView() {
        applyHomeTopChromeInsets()
        binding.swipeHomeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.child_streak_accent),
            ContextCompat.getColor(requireContext(), R.color.child_home_gradient_mid),
        )
        binding.swipeHomeRefresh.setOnChildScrollUpCallback { _, _ ->
            missionDifficultyPicker.isShowing() || binding.scrollPath.canScrollVertically(-1)
        }
        binding.swipeHomeRefresh.setOnRefreshListener {
            missionDifficultyPicker.dismissAnimated()
            viewModel.refreshHomeFromPull()
        }
        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            onOpenDifficultyPicker = { title, nav, anchor, unlockMode ->
                val st = viewModel.uiState.value
                when {
                    nav.missionId.isNotBlank() && !st.isMissionUnlocked(nav.missionId) ->
                        showMissionHint(getString(R.string.quiz_mission_locked))
                    !st.canOpenMissionPicker(unlockMode, viewModel.missionStarLevels(nav.missionId)) ->
                        showEnergyInsufficientSnackbar()
                    else -> {
                        val missionKey = nav.difficultyPickerMissionKey()
                        if (missionDifficultyPicker.isShowingForMission(missionKey)) {
                            missionPickerStarLevelsJob?.cancel()
                            missionDifficultyPicker.dismissAnimated()
                        } else {
                            missionPickerStarLevelsJob?.cancel()
                            val initialStars = viewModel.initialMissionStarLevelsForPicker(nav.missionId)
                                .normalizeToThreeLevels()
                            missionDifficultyPicker.show(
                                title,
                                nav,
                                initialStars,
                                unlockMode,
                                anchor,
                                missionKey,
                            ) { picked, resolvedMode ->
                                missionPickerStarLevelsJob?.cancel()
                                navigateToQuizAfterValidation(picked, resolvedMode)
                            }
                            if (nav.missionId.isNotBlank()) {
                                missionPickerStarLevelsJob = viewLifecycleOwner.lifecycleScope.launch {
                                    val starLevels = viewModel.ensureMissionStarLevels(nav.missionId)
                                    if (!isAdded) return@launch
                                    if (!viewModel.isMissionUnlocked(nav.missionId)) {
                                        missionDifficultyPicker.dismissAnimated()
                                        showMissionHint(getString(R.string.quiz_mission_locked))
                                        return@launch
                                    }
                                    if (missionDifficultyPicker.isShowing()) {
                                        missionDifficultyPicker.updateStarLevels(starLevels, unlockMode, nav)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            onNavigateToQuiz = { nav, unlockMode -> navigateToQuizAfterValidation(nav, unlockMode) },
            onShowMissionHint = { showMissionHint(it) },
            onStageRewardChestClick = { reward -> StageRewardDialog.show(this, reward) },
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
        binding.layoutChipStreak.setOnClickListener { openStreakSheet() }
        parentFragmentManager.setFragmentResultListener(
            QuestListBottomSheet.REQUEST_OPEN_MISSION_LEARN,
            viewLifecycleOwner,
        ) { _, _ ->
            binding.root.post { openMissionLearnFromQuest() }
        }
        childFragmentManager.setFragmentResultListener(
            EnergyBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getBoolean(EnergyBottomSheet.EXTRA_REFRESH_HOME, false)) {
                viewModel.onHomeResumed()
            }
        }
        parentFragmentManager.setFragmentResultListener(
            StreakCalendarBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getBoolean(StreakCalendarBottomSheet.EXTRA_REFRESH_HOME, false)) {
                viewModel.onHomeResumed()
            }
        }
    }

    private fun applyHomeTopChromeInsets() {
        val topColor = ContextCompat.getColor(requireContext(), R.color.child_home_gradient_top)
        requireActivity().window.statusBarColor = topColor
        val baseChromePaddingTop = binding.layoutHomeTopChrome.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.layoutHomeTopChrome.setPadding(
                binding.layoutHomeTopChrome.paddingLeft,
                baseChromePaddingTop + statusTop,
                binding.layoutHomeTopChrome.paddingRight,
                binding.layoutHomeTopChrome.paddingBottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun openMissionLearnFromQuest() {
        viewLifecycleOwner.lifecycleScope.launch {
            val state = viewModel.uiState.first { !it.isLoading }
            openMissionLearnFromQuest(state)
        }
    }

    private fun openMissionLearnFromQuest(state: HomeUiState) {
        val entry = viewModel.resolveQuestLearnEntry()
        if (entry == null) {
            showMissionHint(getString(R.string.mission_no_playable_star_level))
            return
        }
        val (nav, unlockMode) = entry
        if (!nav.canNavigate()) {
            showMissionHint(getString(R.string.mission_no_playable_star_level))
            return
        }
        if (!state.canOpenMissionPicker(unlockMode, viewModel.missionStarLevels(nav.missionId))) {
            showEnergyInsufficientSnackbar()
            return
        }
        navigateToQuizAfterValidation(nav, unlockMode)
    }

    private fun showEnergyInsufficientSnackbar() {
        Snackbar.make(
            binding.root,
            getString(R.string.home_energy_insufficient_toast),
            Snackbar.LENGTH_LONG,
        )
            .setAction(getString(R.string.home_go_energy_charge)) {
                EnergyBottomSheet.newInstance().show(childFragmentManager, "energy_sheet")
            }
            .show()
    }

    private fun navigateToQuizAfterValidation(nav: HomeQuizNavigation, unlockMode: DifficultyUnlockMode) {
        val st = viewModel.uiState.value
        if (!st.canAttemptMissionStart(skipEnergyBecauseReview = unlockMode == DifficultyUnlockMode.REVIEW)) {
            showEnergyInsufficientSnackbar()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.validateMissionQuizNav(nav, unlockMode)
                .onSuccess { validated ->
                    findNavController().navigate(
                        R.id.quizFragment,
                        androidx.core.os.bundleOf(
                            "entrySetId" to validated.entrySetId,
                            "missionId" to validated.missionId,
                            "starLevel" to validated.starLevel,
                        ),
                    )
                }
                .onFailure { e ->
                    Snackbar.make(binding.root, e.message ?: getString(R.string.quiz_star_not_playable), Snackbar.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        missionPickerStarLevelsJob?.cancel()
        missionPickerStarLevelsJob = null
        if (this::homeLayoutBinder.isInitialized) {
            missionDifficultyPicker.dismissImmediate()
        }
        lastHomePathStructureKey = null
        super.onDestroyView()
    }

    private fun openGacha() {
        findNavController().onChildBottomNavTap(R.id.gachaFragment)
    }

    private fun openStreakSheet() {
        val s = viewModel.uiState.value
        val streak = s.streakDays
        StreakCalendarBottomSheet.newInstance(
            fallbackStreakDaysFromHome = streak,
            petType = s.equippedPetType,
            petStage = s.petStage,
            petGrade = s.equippedPetGrade,
        ).show(parentFragmentManager, "streak_calendar")
    }

    private fun showMissionHint(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showPetStatsSheet() {
        val s = viewModel.uiState.value
        if (!s.hasEquippedPet) {
            showMissionHint(getString(R.string.pet_equip_required_for_xp))
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottomsheet_pet_stats, null, false)
        PetStatsSheetUi.bind(
            root = v,
            state = s,
            petNameFallback = getString(R.string.home_pet_name_default),
        )
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
                launch {
                    viewModel.pendingAimongCelebration.collect { pending ->
                        if (pending == null || !isAdded) return@collect
                        binding.root.post {
                            if (!isAdded) return@post
                            AimongCelebrationDialog.show(this@HomeFragment, pending)
                            viewModel.consumeAimongCelebration()
                        }
                    }
                }
                launch {
                    viewModel.pendingStreakShieldRecovery.collect { pending ->
                        if (pending == null || !isAdded) return@collect
                        binding.root.post {
                            if (!isAdded) return@post
                            StreakShieldRecoveryDialog.show(
                                host = this@HomeFragment,
                                ui = pending,
                                onUseShield = { viewModel.useStreakShieldForRecovery() },
                                onDismissForNow = {
                                    viewModel.consumeStreakShieldRecoveryPrompt(dismissForNow = true)
                                },
                            )
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.root.post {
                        val pathKey = state.pathItems.pathStructureKey()
                        if (pathKey != lastHomePathStructureKey) {
                            missionPickerStarLevelsJob?.cancel()
                            missionDifficultyPicker.dismissImmediate()
                            lastHomePathStructureKey = pathKey
                        }
                        binding.swipeHomeRefresh.isRefreshing = state.isRefreshing
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
