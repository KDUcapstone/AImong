package com.kduniv.aimong.feature.home.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.util.DateUtils
import com.kduniv.aimong.feature.dev.mock.MockUiSamples
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v2.7: 에너지 칩 탭 → [GET /energy] 표시, [POST /energy/add] 로 보충 후 홈 갱신 요청.
 */
@AndroidEntryPoint
class EnergyBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var homeRepository: HomeRepository

    @Inject
    lateinit var homeRefreshBus: ChildHomeRefreshBus

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // 바깥 딤만 투명 처리하고, 시트 본문은 레이아웃·코드로 불투명 배경
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            dialog.findViewById<android.widget.FrameLayout>(MaterialR.id.design_bottom_sheet)?.apply {
                background = ContextCompat.getDrawable(context, R.drawable.bg_energy_bottom_sheet)
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_energy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pb = view.findViewById<ProgressBar>(R.id.pb_energy_loading)
        val btnAdd = view.findViewById<AppCompatButton>(R.id.btn_add_energy)
        val tvTitle = view.findViewById<TextView>(R.id.tv_energy_title)
        val tvValue = view.findViewById<TextView>(R.id.tv_energy_value)
        val tvNext = view.findViewById<TextView>(R.id.tv_next_recover)
        val tvFullRecover = view.findViewById<TextView>(R.id.tv_full_recover)
        val tvRecoverInterval = view.findViewById<TextView>(R.id.tv_recover_interval)
        val tvCost = view.findViewById<TextView>(R.id.tv_mission_cost)

        tvTitle.setText(R.string.energy_sheet_title)

        btnAdd.setOnClickListener {
            if (UiMode.useStubNav) {
                MockUiSamples.addMockEnergy(ADD_AMOUNT)
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(EXTRA_REFRESH_HOME to true)
                )
                dismissAllowingStateLoss()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                btnAdd.isEnabled = false
                val result = homeRepository.addEnergy(ADD_AMOUNT)
                if (!isAdded) return@launch
                result.fold(
                    onSuccess = {
                        homeRefreshBus.notify(HomeRefreshTrigger.Full)
                        parentFragmentManager.setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(EXTRA_REFRESH_HOME to true),
                        )
                        dismissAllowingStateLoss()
                    },
                    onFailure = { e ->
                        if (e is CancellationException) return@fold
                        if (!isAdded) return@fold
                        Snackbar.make(
                            requireView(),
                            e.message ?: getString(R.string.energy_add_failed),
                            Snackbar.LENGTH_LONG,
                        ).show()
                        btnAdd.isEnabled = true
                    },
                )
            }
        }

        if (UiMode.useStubNav) {
            pb.visibility = View.GONE
            btnAdd.isEnabled = true
            applyStubEnergyUi(tvValue, tvNext, tvCost, btnAdd)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            pb.visibility = View.VISIBLE
            btnAdd.isEnabled = false
            homeRepository.getEnergy().fold(
                onSuccess = { state ->
                    tvValue.text = getString(R.string.energy_value_fmt, state.energy, state.maxEnergy)
                    val next = state.nextEnergyRecoverAt?.trim()?.takeIf { it.isNotEmpty() }
                    tvNext.text = if (next.isNullOrEmpty()) {
                        getString(R.string.energy_next_recover_unknown)
                    } else {
                        getString(
                            R.string.energy_next_recover_fmt,
                            DateUtils.formatIsoUtcForLocal(next)
                        )
                    }
                    val fullRecover = state.fullRecoverAt?.trim()?.takeIf { it.isNotEmpty() }
                    if (fullRecover.isNullOrEmpty()) {
                        tvFullRecover.visibility = View.GONE
                    } else {
                        tvFullRecover.visibility = View.VISIBLE
                        tvFullRecover.text = getString(
                            R.string.energy_full_recover_fmt,
                            DateUtils.formatIsoUtcForLocal(fullRecover),
                        )
                    }
                    val intervalMin = state.recoverIntervalMinutes
                    if (intervalMin != null && intervalMin > 0) {
                        tvRecoverInterval.visibility = View.VISIBLE
                        tvRecoverInterval.text = getString(R.string.energy_recover_interval_fmt, intervalMin)
                    } else {
                        tvRecoverInterval.visibility = View.GONE
                    }
                    val cost = state.missionStartCost
                    tvCost.text = if (cost != null) {
                        getString(R.string.energy_mission_cost_fmt, cost)
                    } else {
                        ""
                    }
                    tvCost.visibility = if (cost != null) View.VISIBLE else View.GONE
                    val full = state.energy >= state.maxEnergy
                    btnAdd.isEnabled = !full
                    btnAdd.alpha = if (full) 0.45f else 1f
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    tvValue.text = "—"
                    tvNext.text = e.message ?: getString(R.string.energy_load_failed)
                    tvCost.visibility = View.GONE
                    btnAdd.isEnabled = false
                }
            )
            pb.visibility = View.GONE
        }
    }

    private fun applyStubEnergyUi(
        tvValue: TextView,
        tvNext: TextView,
        tvCost: TextView,
        btnAdd: AppCompatButton
    ) {
        val cur = MockUiSamples.mockEnergyCurrent
        val max = MockUiSamples.MOCK_ENERGY_MAX
        tvValue.text = getString(R.string.energy_value_fmt, cur, max)
        tvNext.setText(R.string.mock_energy_next_recover_hint)
        tvCost.text = getString(R.string.energy_mission_cost_fmt, 5)
        tvCost.visibility = View.VISIBLE
        val full = cur >= max
        btnAdd.isEnabled = !full
        btnAdd.alpha = if (full) 0.45f else 1f
    }

    companion object {
        const val REQUEST_KEY = "aimong_energy_sheet"
        const val EXTRA_REFRESH_HOME = "refresh_home"
        private const val ADD_AMOUNT = 5

        fun newInstance(): EnergyBottomSheet = EnergyBottomSheet()
    }
}
