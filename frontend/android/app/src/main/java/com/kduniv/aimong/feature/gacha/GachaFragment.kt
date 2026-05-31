package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.ui.CelebrationDialogWindow
import com.kduniv.aimong.core.ui.TutorialCoachmarkOverlay
import com.kduniv.aimong.databinding.DialogGachaPetDetailBinding
import com.kduniv.aimong.databinding.FragmentGachaBinding
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingController
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingDialogs
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingPhase
import com.kduniv.aimong.feature.pet.data.model.PetDto
import com.kduniv.aimong.feature.pet.data.model.PetListData
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlin.math.max

@AndroidEntryPoint
class GachaFragment : BaseFragment<FragmentGachaBinding>(FragmentGachaBinding::inflate) {

    @Inject
    lateinit var childGachaOnboardingController: ChildGachaOnboardingController

    private val viewModel: GachaViewModel by viewModels()
    private lateinit var petAdapter: GachaPetAdapter
    private var pullRevealShowing = false
    private var lastEquippedBindKey: String? = null
    private var lastPetListRevisionKey: String? = null
    private var coachmarkOverlay: TutorialCoachmarkOverlay? = null
    private var onboardingCompleteDialogShown = false

    override fun onResume() {
        super.onResume()
        if (childGachaOnboardingController.isActive) {
            viewModel.syncTicketsFromHome()
            viewModel.seedTicketsIfEmpty(childGachaOnboardingController.onboardingTicketHint)
        } else {
            viewModel.onGachaResumed()
        }
    }

    override fun onDestroyView() {
        coachmarkOverlay?.dismiss()
        coachmarkOverlay = null
        super.onDestroyView()
    }

    override fun initView() {
        coachmarkOverlay = TutorialCoachmarkOverlay(binding.layoutGachaRoot)
        val onPetClick: (GachaPetCardUi) -> Unit = { item ->
            if (childGachaOnboardingController.phase.value == ChildGachaOnboardingPhase.GachaEquipCoachmark &&
                item.isLocked
            ) {
                Snackbar.make(binding.root, R.string.child_onboarding_coach_equip, Snackbar.LENGTH_SHORT).show()
            } else {
                showPetDetailDialog(item)
            }
        }

        petAdapter = GachaPetAdapter(onPetClick)
        val encyclopediaSpan = resources.getInteger(R.integer.gacha_encyclopedia_span_count)
        val gridSpacing = resources.getDimensionPixelSize(R.dimen.gacha_encyclopedia_grid_spacing)
        binding.rvPets.layoutManager = GridLayoutManager(requireContext(), encyclopediaSpan)
        if (binding.rvPets.itemDecorationCount == 0) {
            binding.rvPets.addItemDecoration(
                GachaGridSpacingDecoration(encyclopediaSpan, gridSpacing),
            )
        }
        binding.rvPets.adapter = petAdapter
        binding.rvPets.setHasFixedSize(true)

        applyWindowInsets()

        binding.btnProbabilities.setOnClickListener {
            if (childGachaOnboardingController.isActive) return@setOnClickListener
            showProbabilitySheet()
        }

        binding.btnPull.setOnClickListener {
            if (viewModel.state.value.hasAnyTicket) {
                viewModel.pull()
            } else {
                Snackbar.make(binding.root, R.string.gacha_ticket_insufficient, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyWindowInsets() {
        val baseTopPadding = binding.headerContainer.paddingTop
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.layoutGachaRoot) { _, insets ->
            val top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            binding.headerContainer.setPadding(
                binding.headerContainer.paddingLeft,
                baseTopPadding + top,
                binding.headerContainer.paddingRight,
                binding.headerContainer.paddingBottom
            )
            insets
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    childGachaOnboardingController.phase.collect { phase ->
                        when (phase) {
                            ChildGachaOnboardingPhase.GachaPullCoachmark -> {
                                viewModel.seedTicketsIfEmpty(
                                    childGachaOnboardingController.onboardingTicketHint,
                                )
                                viewModel.refresh()
                                binding.root.post { showPullCoachmarkIfNeeded() }
                            }
                            ChildGachaOnboardingPhase.GachaEquipCoachmark ->
                                binding.rvPets.post { showEquipCoachmarkIfNeeded() }
                            else -> coachmarkOverlay?.dismiss()
                        }
                    }
                }
                viewModel.state.collect { s ->
                    binding.pbGacha.isVisible = s.loading && !pullRevealShowing

                    val eq = s.pets?.equippedPet
                    val hasEquipped = eq != null
                    binding.layoutEquippedPet.isVisible = hasEquipped
                    binding.tvEquipEmpty.isVisible = !hasEquipped
                    binding.tvEquipBanner.isVisible = !hasEquipped

                    resolveEquippedPet(s.pets)?.let { equipped ->
                        val bindKey = "${equipped.id}|${equipped.petType}|${equipped.stage}"
                        if (bindKey != lastEquippedBindKey) {
                            lastEquippedBindKey = bindKey
                            val equippedStage = com.kduniv.aimong.feature.pet.domain.PetGrowthRules
                                .resolveEffectiveStageString(equipped.stage, equipped.xp)
                            PetArtAssets.bindEquipped(
                                image = binding.ivEquippedSprite,
                                emojiFallback = binding.tvEquippedEmoji,
                                petType = equipped.petType,
                                stage = equippedStage,
                                grade = equipped.grade,
                            )
                        }
                        binding.tvEquippedName.text = GachaUiMapper.displayName(equipped)
                        val stageLabel = equippedStageSubtitle(equipped)
                        binding.tvEquippedStage.isVisible = stageLabel.isNotBlank()
                        binding.tvEquippedStage.text = stageLabel
                    }

                    binding.tvTicketNormal.text = s.normalTicketCount.toString()
                    bindGachaLevelStrip(s.gachaPullCount)

                    binding.tvEncyclopediaProgress.text = getString(
                        R.string.gacha_encyclopedia_progress_fmt,
                        s.ownedCatalogCount,
                        GachaPetCatalog.TOTAL
                    )

                    val listRevisionKey = buildString {
                        append(s.fragmentBalance.totalCount)
                        append('|')
                        append(s.petCards.size)
                        append('|')
                        append(s.ownedCatalogCount)
                        append('|')
                        append(s.pets?.equippedPet?.id.orEmpty())
                        append('|')
                        s.pets?.pets.orEmpty().forEach { p ->
                            append(p.id)
                            append(':')
                            append(p.stage)
                            append(':')
                            append(p.xp)
                            append(';')
                        }
                        s.petCards.forEach { c ->
                            append(c.catalogPetType)
                            append(':')
                            append(c.fragmentCount)
                            append('/')
                            append(c.fragmentThreshold)
                            append('|')
                        }
                    }
                    if (listRevisionKey != lastPetListRevisionKey) {
                        lastPetListRevisionKey = listRevisionKey
                        petAdapter.submitList(s.petCards)
                    }
                    bindPullButton(s)

                    if (childGachaOnboardingController.phase.value ==
                        ChildGachaOnboardingPhase.GachaEquipCoachmark &&
                        s.pets?.equippedPet != null
                    ) {
                        maybeCompleteOnboarding()
                    }

                    s.transientMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeTransientMessage()
                    }

                    s.pullReveal?.let { reveal ->
                        if (!pullRevealShowing) {
                            pullRevealShowing = true
                            GachaPullRevealPresenter.show(
                                fragment = this@GachaFragment,
                                reveal = reveal,
                                onDismiss = {
                                    pullRevealShowing = false
                                    viewModel.consumePullReveal()
                                    if (childGachaOnboardingController.phase.value ==
                                        ChildGachaOnboardingPhase.GachaPullCoachmark
                                    ) {
                                        childGachaOnboardingController.onGachaEquipCoachmark()
                                        binding.rvPets.post { showEquipCoachmarkIfNeeded() }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindGachaLevelStrip(pullCount: Int) {
        val progress = GachaProbabilityTable.levelProgress(pullCount)
        binding.tvGachaLevel.text = getString(R.string.gacha_prob_level_fmt, progress.level)
        if (progress.isMaxLevel) {
            binding.pbGachaLevelProgress.progress = 100
            binding.tvGachaLevelProgress.text = getString(
                R.string.gacha_level_progress_max_fmt,
                pullCount,
            )
        } else {
            val required = progress.requiredInLevel.coerceAtLeast(1)
            binding.pbGachaLevelProgress.progress =
                (progress.currentInLevel * 100 / required).coerceIn(0, 100)
            binding.tvGachaLevelProgress.text = getString(
                R.string.gacha_level_progress_fmt,
                progress.currentInLevel,
                required,
            )
        }
    }

    private fun bindPullButton(s: GachaViewModel.UiState) {
        val enabled = s.hasAnyTicket && !pullRevealShowing
        binding.btnPull.isEnabled = enabled
        binding.btnPull.alpha = if (enabled) 1f else 0.5f
        binding.btnPull.setBackgroundResource(
            if (enabled) R.drawable.bg_gacha_pull_enabled else R.drawable.bg_gacha_pull_disabled
        )
    }

    private fun showPullCoachmarkIfNeeded() {
        if (childGachaOnboardingController.phase.value != ChildGachaOnboardingPhase.GachaPullCoachmark) {
            return
        }
        if (!binding.btnPull.isShown) {
            binding.btnPull.post { showPullCoachmarkIfNeeded() }
            return
        }
        coachmarkOverlay?.show(
            binding.btnPull,
            getString(R.string.child_onboarding_coach_pull),
        )
    }

    private fun showEquipCoachmarkIfNeeded() {
        if (childGachaOnboardingController.phase.value != ChildGachaOnboardingPhase.GachaEquipCoachmark) {
            return
        }
        val index = viewModel.state.value.petCards.indexOfFirst { !it.isLocked }
        if (index < 0) {
            binding.rvPets.postDelayed({ showEquipCoachmarkIfNeeded() }, 150)
            return
        }
        (binding.rvPets.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(index, 48)
        binding.rvPets.post {
            val holder = binding.rvPets.findViewHolderForAdapterPosition(index)
            val target = holder?.itemView ?: binding.rvPets
            coachmarkOverlay?.show(target, getString(R.string.child_onboarding_coach_equip))
        }
    }

    private fun maybeCompleteOnboarding() {
        if (onboardingCompleteDialogShown) return
        if (childGachaOnboardingController.phase.value != ChildGachaOnboardingPhase.GachaEquipCoachmark) {
            return
        }
        onboardingCompleteDialogShown = true
        coachmarkOverlay?.dismiss()
        childGachaOnboardingController.onCompleting()
        viewLifecycleOwner.lifecycleScope.launch {
            childGachaOnboardingController.refreshEquippedFromServer()
            if (!isAdded) return@launch
            ChildGachaOnboardingDialogs.showComplete(this@GachaFragment) {
                (activity as? MainActivity)?.navigateChildToHomeAfterOnboarding()
            }
        }
    }

    private fun showPetDetailDialog(item: GachaPetCardUi) {
        coachmarkOverlay?.dismiss()
        val ctx = requireContext()
        val dialogBinding =
            DialogGachaPetDetailBinding.inflate(LayoutInflater.from(ctx))

        val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        if (item.isLocked) {
            showLockedPetDetail(dialogBinding, item, dialog)
        } else {
            showOwnedPetDetail(dialogBinding, item, dialog)
        }

        dialog.show()
        CelebrationDialogWindow.apply(dialog, ctx, dimAmount = 0.45f)
    }

    private fun showLockedPetDetail(
        dialogBinding: DialogGachaPetDetailBinding,
        item: GachaPetCardUi,
        dialog: AlertDialog,
    ) {
        dialogBinding.tvPetGrowth.isVisible = false
        PetArtAssets.bindSprite(
            image = dialogBinding.ivPetSprite,
            emojiFallback = dialogBinding.tvPetEmoji,
            petType = item.catalogPetType,
            stage = "EGG",
            emoji = item.emoji,
        )
        dialogBinding.tvPetName.text = item.displayName
        dialogBinding.tvPetGrade.text = getString(
            R.string.gacha_pet_grade_fmt,
            GachaUiMapper.gradeLabel(item.grade)
        )

        dialogBinding.layoutFragmentExchange.isVisible = true
        dialogBinding.btnEquip.isVisible = false
        dialogBinding.btnClose.isVisible = true

        val threshold = item.fragmentThreshold.coerceAtLeast(1)
        val count = item.fragmentCount.coerceAtLeast(0)
        val progress = ((count.toFloat() / threshold) * 100f).toInt().coerceIn(0, 100)
        dialogBinding.pbFragments.progress = progress
        dialogBinding.tvFragmentCount.text =
            getString(R.string.gacha_fragment_progress_fmt, count, threshold)

        if (item.canExchange) {
            dialogBinding.btnExchange.isVisible = true
            dialogBinding.btnExchange.isEnabled = true
            dialogBinding.tvExchangeHint.text = getString(R.string.gacha_exchange_manual_hint)
            dialogBinding.btnExchange.setOnClickListener {
                viewModel.exchange(item.grade, item.catalogPetType)
                dialog.dismiss()
            }
        } else {
            dialogBinding.btnExchange.isVisible = false
            val need = max(threshold - count, 0)
            dialogBinding.tvExchangeHint.text = getString(
                R.string.gacha_exchange_need_more_fmt,
                need,
                count,
                threshold,
            )
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
    }

    private fun showOwnedPetDetail(
        dialogBinding: DialogGachaPetDetailBinding,
        item: GachaPetCardUi,
        dialog: AlertDialog,
    ) {
        val pet = item.pet ?: return
        val ctx = dialogBinding.root.context

        val effectiveStage = com.kduniv.aimong.feature.pet.domain.PetGrowthRules
            .resolveEffectiveStageString(pet.stage, pet.xp)
        PetArtAssets.bindSprite(
            image = dialogBinding.ivPetSprite,
            emojiFallback = dialogBinding.tvPetEmoji,
            petType = pet.petType,
            stage = effectiveStage,
            emoji = item.emoji,
            allowStageFallback = false,
        )
        dialogBinding.tvPetName.text = item.displayName
        dialogBinding.tvPetGrade.text = getString(
            R.string.gacha_pet_grade_fmt,
            GachaUiMapper.gradeLabel(pet.grade),
        )
        val growthLine = GachaUiMapper.displayPetGrowthDetail(requireContext(), pet)
        dialogBinding.tvPetGrowth.isVisible = growthLine.isNotBlank()
        dialogBinding.tvPetGrowth.text = growthLine

        dialogBinding.layoutFragmentExchange.isVisible = false
        dialogBinding.btnExchange.isVisible = false
        dialogBinding.btnEquip.isVisible = true
        dialogBinding.btnClose.isVisible = true
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        if (item.isEquipped) {
            dialogBinding.btnEquip.isEnabled = false
            dialogBinding.btnEquip.text = getString(R.string.gacha_equipped_now)
            dialogBinding.btnEquip.setBackgroundResource(R.drawable.bg_gacha_action_disabled)
            dialogBinding.btnEquip.setTextColor(
                ContextCompat.getColor(ctx, R.color.quiz_text_secondary),
            )
        } else {
            dialogBinding.btnEquip.setOnClickListener {
                dialog.dismiss()
                binding.root.post { viewModel.equipPet(pet.id) }
            }
        }
    }

    private fun showProbabilitySheet() {
        val level = viewModel.state.value.gachaLevel
        GachaProbabilityDialog.show(this, level)
    }

    /** equippedPet 요약과 도감 카드가 동일 PetDto 를 쓰도록 목록에서 재조회 */
    private fun resolveEquippedPet(pets: PetListData?): PetDto? {
        val summary = pets?.equippedPet ?: return null
        return pets.pets.firstOrNull { it.id == summary.id } ?: summary
    }

    private fun equippedStageSubtitle(pet: PetDto): String =
        GachaUiMapper.displayPetGrowthDetail(requireContext(), pet)
}
