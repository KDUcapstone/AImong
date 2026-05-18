package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.BottomSheetGachaProbabilitiesBinding
import com.kduniv.aimong.databinding.BottomSheetGachaTicketBinding
import com.kduniv.aimong.databinding.DialogGachaPetDetailBinding
import com.kduniv.aimong.databinding.FragmentGachaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GachaFragment : BaseFragment<FragmentGachaBinding>(FragmentGachaBinding::inflate) {

    private val viewModel: GachaViewModel by viewModels()
    private lateinit var petAdapter: GachaPetAdapter
    private lateinit var ownedPetAdapter: GachaOwnedPetAdapter

    override fun initView() {
        val onPetClick: (GachaPetCardUi) -> Unit = { item -> showPetDetailDialog(item) }

        petAdapter = GachaPetAdapter(onPetClick)
        binding.rvPets.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvPets.adapter = petAdapter
        binding.rvPets.setHasFixedSize(true)

        ownedPetAdapter = GachaOwnedPetAdapter(onPetClick)
        binding.rvOwnedPets.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvOwnedPets.adapter = ownedPetAdapter

        applyWindowInsets()

        binding.btnProbabilities.bringToFront()
        binding.btnProbabilities.setOnClickListener { showProbabilitySheet() }

        binding.btnPull.setOnClickListener {
            if (viewModel.state.value.hasAnyTicket) {
                showTicketPickerSheet()
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
                viewModel.state.collect { s ->
                    binding.pbGacha.isVisible = s.loading

                    val eq = s.pets?.equippedPet
                    val hasEquipped = eq != null
                    binding.layoutEquippedPet.isVisible = hasEquipped
                    binding.tvEquipEmpty.isVisible = !hasEquipped
                    binding.tvEquipBanner.isVisible = !hasEquipped

                    if (eq != null) {
                        val catalogEntry =
                            GachaPetCatalog.entries.firstOrNull { it.petType == eq.petType }
                        binding.tvEquippedEmoji.text =
                            catalogEntry?.emoji ?: GachaUiMapper.petEmoji(eq)
                        binding.tvEquippedName.text =
                            catalogEntry?.displayName ?: GachaUiMapper.displayName(eq)
                    }

                    val tix = s.tickets
                    binding.tvTicketNormal.text = (tix?.normal ?: 0).toString()
                    binding.tvTicketRare.text = (tix?.rare ?: 0).toString()
                    binding.tvTicketEpic.text = (tix?.epic ?: 0).toString()

                    val hasOwned = s.ownedPetCards.isNotEmpty()
                    binding.rvOwnedPets.isVisible = hasOwned
                    binding.tvOwnedEmpty.isVisible = !hasOwned
                    ownedPetAdapter.submitList(s.ownedPetCards)

                    binding.tvEncyclopediaProgress.text = getString(
                        R.string.gacha_encyclopedia_progress_fmt,
                        s.ownedCatalogCount,
                        GachaPetCatalog.TOTAL
                    )

                    petAdapter.submitList(s.petCards)
                    bindPullButton(s)

                    s.transientMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeTransientMessage()
                    }

                    s.pullSummary?.let { summary ->
                        Snackbar.make(binding.root, summary, Snackbar.LENGTH_LONG).show()
                        viewModel.consumePullSummary()
                    }
                }
            }
        }
    }

    private fun bindPullButton(s: GachaViewModel.UiState) {
        val enabled = s.hasAnyTicket
        binding.btnPull.isEnabled = enabled
        binding.btnPull.alpha = if (enabled) 1f else 0.5f
        binding.btnPull.setBackgroundResource(
            if (enabled) R.drawable.bg_gacha_pull_enabled else R.drawable.bg_gacha_pull_disabled
        )
    }

    private fun showPetDetailDialog(item: GachaPetCardUi) {
        val pet = item.pet ?: return
        val dialogBinding =
            DialogGachaPetDetailBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.tvPetEmoji.text = item.emoji
        dialogBinding.tvPetName.text = item.displayName
        dialogBinding.tvPetGrade.text = getString(
            R.string.gacha_pet_grade_fmt,
            GachaUiMapper.gradeLabel(item.grade)
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        if (item.isEquipped) {
            dialogBinding.btnEquip.isEnabled = false
            dialogBinding.btnEquip.text = getString(R.string.gacha_equipped_now)
        } else {
            dialogBinding.btnEquip.setOnClickListener {
                viewModel.equipPet(pet.id)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showTicketPickerSheet() {
        val state = viewModel.state.value
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetGachaTicketBinding.inflate(LayoutInflater.from(requireContext()))
        sheet.setContentView(sheetBinding.root)

        fun bindPickButton(
            button: com.google.android.material.button.MaterialButton,
            type: String,
            labelRes: Int
        ) {
            val count = state.ticketCount(type)
            button.text = getString(labelRes, count)
            button.isEnabled = count > 0
            button.alpha = if (count > 0) 1f else 0.45f
            button.setOnClickListener {
                viewModel.setTicketType(type)
                sheet.dismiss()
                viewModel.pull()
            }
        }

        bindPickButton(sheetBinding.btnPickNormal, "NORMAL", R.string.gacha_ticket_pick_normal_fmt)
        bindPickButton(sheetBinding.btnPickRare, "RARE", R.string.gacha_ticket_pick_rare_fmt)
        bindPickButton(sheetBinding.btnPickEpic, "EPIC", R.string.gacha_ticket_pick_epic_fmt)

        sheet.show()
    }

    private fun showProbabilitySheet() {
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding =
            BottomSheetGachaProbabilitiesBinding.inflate(LayoutInflater.from(requireContext()))
        sheetBinding.tvProbabilitiesBody.text = getString(R.string.gacha_probabilities_body)
        sheet.setContentView(sheetBinding.root)
        sheet.show()
    }
}
