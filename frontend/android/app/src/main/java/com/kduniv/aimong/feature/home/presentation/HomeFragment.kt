package com.kduniv.aimong.feature.home.presentation

import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.BuildConfig
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.onChildBottomNavTap
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.UiPerfLog
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.feature.home.domain.ChildHomeBootstrapGate
import com.kduniv.aimong.feature.mission.domain.model.normalizeToThreeLevels
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingDialogs
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingController
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingEntry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    @Inject
    lateinit var childGachaOnboardingController: ChildGachaOnboardingController

    @Inject
    lateinit var childHomeBootstrapGate: ChildHomeBootstrapGate

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var homeLayoutBinder: HomeLayoutBinder
    private val missionDifficultyPicker: MissionDifficultyPicker by lazy {
        MissionDifficultyPicker(binding, layoutInflater)
    }
    private var missionPickerStarLevelsJob: Job? = null
    private var lastHomePathStructureKey: String? = null
    private var homePerfResumeMark: Long? = null
    private var homePerfFirstTouchLogged = false
    /** 같은 자녀에 대해 Skip 확정 후에는 재시도하지 않음. NoTickets는 onResume에서 재시도. */
    private var childOnboardingSkippedForChildId: String? = null

    override fun onResume() {
        super.onResume()
        dismissMissionDifficultyPickerForTabLeave()
        homePerfResumeMark = UiPerfLog.mark("home_first_interaction")
        homePerfFirstTouchLogged = false
        viewModel.onHomeResumed()
        if (!UiMode.useStubNav) {
            viewLifecycleOwner.lifecycleScope.launch {
                tryStartChildGachaOnboardingWhenReady()
            }
        }
        viewModel.pendingAimongCelebration.value?.let { pending ->
            binding.root.post {
                if (!isAdded) return@post
                AimongCelebrationDialog.show(this, pending)
                viewModel.consumeAimongCelebration()
            }
        }
    }

    override fun onPause() {
        dismissMissionDifficultyPickerForTabLeave()
        super.onPause()
    }

    /** 다른 탭·화면 이탈 시 팝업을 닫고, 복귀 후에는 노드를 다시 눌러야 연다. */
    private fun dismissMissionDifficultyPickerForTabLeave() {
        if (!missionDifficultyPicker.isShowing()) return
        missionPickerStarLevelsJob?.cancel()
        missionPickerStarLevelsJob = null
        missionDifficultyPicker.dismissImmediate()
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
            onMissionPathWillRebuild = { missionDifficultyPicker.dismissImmediate() },
            onOpenDifficultyPicker = { title, nav, anchor, unlockMode ->
                val st = viewModel.uiState.value
                when {
                    nav.missionId.isNotBlank() && !st.isMissionUnlocked(nav.missionId) ->
                        showMissionHint(getString(R.string.quiz_mission_locked))
                    else -> {
                        val missionKey = nav.difficultyPickerMissionKey()
                        if (missionDifficultyPicker.isShowingForMission(missionKey)) {
                            missionPickerStarLevelsJob?.cancel()
                            missionDifficultyPicker.dismissAnimated()
                        } else {
                            missionPickerStarLevelsJob?.cancel()
                            missionDifficultyPicker.dismissImmediate()
                            val initialStars = viewModel.initialMissionStarLevelsForPicker(nav.missionId)
                                .normalizeToThreeLevels()
                            val pickerMark = UiPerfLog.mark("home_difficulty_picker")
                            val shown = missionDifficultyPicker.show(
                                title,
                                nav,
                                initialStars,
                                unlockMode,
                                anchor,
                                missionKey,
                                onPicked = { picked, resolvedMode ->
                                    missionPickerStarLevelsJob?.cancel()
                                    if (BuildConfig.DEBUG) {
                                        UiPerfLog.mark(
                                            "home_difficulty_pick mission=${picked.missionId} star=${picked.starLevel}",
                                        )
                                    }
                                    navigateToQuizAfterValidation(picked, resolvedMode)
                                },
                                onPopupLaidOut = {
                                    UiPerfLog.measureFrom(
                                        "home_difficulty_picker",
                                        pickerMark,
                                        "home_difficulty_popup_visible_ms",
                                    )
                                    if (nav.missionId.isNotBlank()) {
                                        missionPickerStarLevelsJob?.cancel()
                                        missionPickerStarLevelsJob =
                                            viewLifecycleOwner.lifecycleScope.launch {
                                                val starLevels =
                                                    viewModel.ensureMissionStarLevels(nav.missionId)
                                                if (!isAdded) return@launch
                                                if (!viewModel.isMissionUnlocked(nav.missionId)) {
                                                    missionDifficultyPicker.dismissAnimated()
                                                    showMissionHint(getString(R.string.quiz_mission_locked))
                                                    return@launch
                                                }
                                                if (missionDifficultyPicker.isShowing()) {
                                                    missionDifficultyPicker.updateStarLevels(
                                                        starLevels,
                                                        unlockMode,
                                                        nav,
                                                    )
                                                }
                                            }
                                    }
                                },
                            )
                            if (!shown) {
                                showMissionHint(getString(R.string.home_difficulty_picker_failed))
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
        binding.scrollPath.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                logHomeFirstTouchIfNeeded()
            }
            false
        }
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
        updateHomeBootstrapOverlay(viewModel.uiState.value)
    }

    /** 첫 홈 로드: 앱 아이콘만 보이고, API·경로 준비 후 홈 UI·하단 탭을 한 번에 표시 */
    private fun updateHomeBootstrapOverlay(state: HomeUiState) {
        val showBootstrap =
            state.isLoading && state.pathItems.isEmpty() && state.errorMessage.isNullOrBlank()
        binding.layoutHomeBootstrap.root.isVisible = showBootstrap
        childHomeBootstrapGate.setSuppressChildBottomNav(showBootstrap)
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
            viewModel.uiState.first { !it.isLoading }
            openMissionLearnFromQuestAfterLoad()
        }
    }

    private suspend fun openMissionLearnFromQuestAfterLoad() {
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
                    val msg = e.message.orEmpty()
                    if (msg == getString(R.string.quiz_insufficient_energy)) {
                        showEnergyInsufficientSnackbar()
                    } else {
                        Snackbar.make(
                            binding.root,
                            msg.ifBlank { getString(R.string.quiz_star_not_playable) },
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        childHomeBootstrapGate.setSuppressChildBottomNav(false)
        missionPickerStarLevelsJob?.cancel()
        missionPickerStarLevelsJob = null
        if (this::homeLayoutBinder.isInitialized) {
            missionDifficultyPicker.dismissImmediate()
        }
        lastHomePathStructureKey = null
        childOnboardingSkippedForChildId = null
        super.onDestroyView()
    }

    private fun logHomeFirstTouchIfNeeded() {
        if (homePerfFirstTouchLogged) return
        homePerfFirstTouchLogged = true
        homePerfResumeMark?.let { started ->
            UiPerfLog.measureFrom(
                "home_first_interaction",
                started,
                "home_resume_to_first_touch_ms",
            )
            homePerfResumeMark = null
        }
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
        PetStatsSheetPresenter.show(this, s)
    }

    private fun openQuestList() {
        QuestListBottomSheet.newInstance(canStartMission = viewModel.uiState.value.canStartMission)
            .show(parentFragmentManager, "quest_list")
    }

    private suspend fun tryStartChildGachaOnboardingWhenReady() {
        if (!isAdded || UiMode.useStubNav) return
        val state = viewModel.uiState.value
        if (state.isLoading && state.pathItems.isEmpty()) return
        val childId = viewModel.currentChildId()
            ?: state.childId.takeIf { it.isNotBlank() }
            ?: return
        if (childOnboardingSkippedForChildId == childId) return

        val profileChildId = state.childId.takeIf { it.isNotBlank() } ?: childId
        when (val entry = childGachaOnboardingController.evaluateEntry(
            homeTicketHint = state.normalTickets,
            profileChildId = profileChildId,
        )) {
            ChildGachaOnboardingEntry.Skip -> {
                childOnboardingSkippedForChildId = childId
            }
            ChildGachaOnboardingEntry.NoTickets -> {
                if (!isAdded) return
                binding.root.post {
                    if (!isAdded) return@post
                    ChildGachaOnboardingDialogs.showNoTickets(this@HomeFragment)
                }
            }
            is ChildGachaOnboardingEntry.StartWelcome -> {
                childOnboardingSkippedForChildId = childId
                if (!isAdded) return
                binding.root.post {
                    if (!isAdded) return@post
                    childGachaOnboardingController.onWelcomeShown()
                    ChildGachaOnboardingDialogs.showWelcome(
                        host = this@HomeFragment,
                        ticketCount = entry.ticketCount,
                    ) {
                        val activity = activity as? MainActivity ?: return@showWelcome
                        activity.navigateChildToGachaForOnboarding()
                    }
                }
            }
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    if (!UiMode.useStubNav) {
                        tryStartChildGachaOnboardingWhenReady()
                    }
                }
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
                            lastHomePathStructureKey = pathKey
                            // 패널이 열려 있을 때는 경로 바인더가 처리 — 여기서 닫으면 열리자마자 사라짐
                            if (!missionDifficultyPicker.isShowing()) {
                                missionDifficultyPicker.dismissImmediate()
                            }
                        }
                        binding.swipeHomeRefresh.isRefreshing = state.isRefreshing
                        updateHomeBootstrapOverlay(state)
                        if (!binding.layoutHomeBootstrap.root.isVisible) {
                            homeLayoutBinder.bind(state)
                        }
                        if (!UiMode.useStubNav &&
                            !binding.layoutHomeBootstrap.root.isVisible &&
                            !state.isLoading
                        ) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                tryStartChildGachaOnboardingWhenReady()
                            }
                        }
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
