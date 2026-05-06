package com.kduniv.aimong.feature.gacha

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentGachaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GachaFragment : BaseFragment<FragmentGachaBinding>(FragmentGachaBinding::inflate) {

    private val viewModel: GachaViewModel by viewModels()
    private lateinit var petAdapter: GachaPetAdapter

    override fun initView() {
        petAdapter = GachaPetAdapter { petId -> viewModel.equipPet(petId) }
        binding.rvPets.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvPets.adapter = petAdapter

        // 상단바 인셋만큼 전체를 아래로
        val baseTopPadding = binding.layoutGachaRoot.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollGacha) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.layoutGachaRoot.setPadding(
                binding.layoutGachaRoot.paddingLeft,
                baseTopPadding + top,
                binding.layoutGachaRoot.paddingRight,
                binding.layoutGachaRoot.paddingBottom
            )
            insets
        }

        binding.chipsTicket.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull()
            val type = when (id) {
                R.id.chip_ticket_rare -> "RARE"
                R.id.chip_ticket_epic -> "EPIC"
                else -> "NORMAL"
            }
            viewModel.setTicketType(type)
        }

        binding.btnPull.setOnClickListener { viewModel.pull() }
        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.btnProbabilities.setOnClickListener { showProbabilityDialog() }
        binding.btnExchange.setOnClickListener { showExchangeDialog() }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.pbGacha.isVisible = s.loading
                    binding.tvEquipBanner.isVisible = s.pets?.equippedPet == null

                    val eq = s.pets?.equippedPet
                    binding.tvEquippedSummary.text = if (eq != null) {
                        getString(
                            R.string.gacha_equipped_fmt,
                            eq.petType,
                            eq.grade,
                            eq.stage,
                            eq.xp
                        )
                    } else {
                        ""
                    }

                    val tix = s.lastRemainingTickets
                    binding.tvTicketsLine.text = if (tix != null) {
                        getString(R.string.gacha_tickets_fmt, tix.normal, tix.rare, tix.epic)
                    } else {
                        getString(R.string.gacha_tickets_unknown)
                    }

                    binding.tvPullResult.text = s.pullSummary.orEmpty()
                    binding.tvFragmentsBody.text = s.fragmentsText

                    petAdapter.equippedPetId = s.pets?.equippedPet?.id
                    petAdapter.submitList(s.pets?.pets.orEmpty())

                    s.transientMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeTransientMessage()
                    }
                }
            }
        }
    }

    private fun showProbabilityDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.gacha_probabilities_title))
            .setMessage(getString(R.string.gacha_probabilities_body))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showExchangeDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_gacha_exchange, null, false)
        val gradeEt = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_exchange_grade)
        val typeEt = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_exchange_pet_type)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.gacha_exchange_title))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.gacha_exchange_button) { _, _ ->
                val grade = gradeEt.text?.toString().orEmpty()
                val petType = typeEt.text?.toString().orEmpty()
                viewModel.exchange(grade.trim(), petType.trim())
            }
            .show()
    }
}
