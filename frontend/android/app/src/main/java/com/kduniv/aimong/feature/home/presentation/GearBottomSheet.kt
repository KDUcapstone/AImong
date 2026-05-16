package com.kduniv.aimong.feature.home.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.dev.mock.MockGearBalance
import com.kduniv.aimong.feature.streak.data.StreakRepository
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v1.1: [GET /wallet] 잔액·비용 표시, [POST /streak/shields/purchase] 보호권 구매.
 */
@AndroidEntryPoint
class GearBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var walletRepository: WalletRepository
    @Inject lateinit var streakRepository: StreakRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
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
    ): View = inflater.inflate(R.layout.bottom_sheet_gear, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pb = view.findViewById<ProgressBar>(R.id.pb_gear_loading)
        val btnBuy = view.findViewById<AppCompatButton>(R.id.btn_buy_streak_shield)
        val tvValue = view.findViewById<TextView>(R.id.tv_gear_value)
        val tvHeart = view.findViewById<TextView>(R.id.tv_gear_heart_cost)
        val tvShield = view.findViewById<TextView>(R.id.tv_gear_shield_cost)

        btnBuy.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                btnBuy.isEnabled = false
                streakRepository.purchaseShield(1).fold(
                    onSuccess = {
                        Snackbar.make(view, getString(R.string.gear_shield_purchase_success), Snackbar.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(EXTRA_REFRESH_HOME to true)
                        )
                        loadWallet(tvValue, tvHeart, tvShield, btnBuy, pb)
                    },
                    onFailure = { e ->
                        Snackbar.make(view, e.message ?: getString(R.string.gear_shield_purchase_failed), Snackbar.LENGTH_LONG).show()
                        btnBuy.isEnabled = true
                    }
                )
            }
        }

        loadWallet(tvValue, tvHeart, tvShield, btnBuy, pb)
    }

    private fun loadWallet(
        tvValue: TextView,
        tvHeart: TextView,
        tvShield: TextView,
        btnBuy: AppCompatButton,
        pb: ProgressBar
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            pb.visibility = View.VISIBLE
            btnBuy.isEnabled = false
            walletRepository.getWallet().fold(
                onSuccess = { wallet ->
                    tvValue.text = getString(R.string.gear_value_fmt, wallet.gear)
                    tvHeart.text = getString(R.string.gear_heart_revive_cost_fmt, wallet.heartReviveCost)
                    tvShield.text = getString(R.string.gear_streak_shield_cost_fmt, wallet.streakShieldCost)
                    val canBuy = if (UiMode.useStubNav) {
                        MockGearBalance.gear >= wallet.streakShieldCost
                    } else {
                        wallet.gear >= wallet.streakShieldCost
                    }
                    btnBuy.isEnabled = canBuy
                    btnBuy.alpha = if (canBuy) 1f else 0.45f
                    btnBuy.text = getString(
                        R.string.gear_buy_streak_shield_cost_fmt,
                        wallet.streakShieldCost,
                        wallet.gear
                    )
                },
                onFailure = { e ->
                    tvValue.text = "—"
                    tvHeart.text = e.message ?: getString(R.string.gear_load_failed)
                    tvShield.text = ""
                    btnBuy.isEnabled = false
                }
            )
            pb.visibility = View.GONE
        }
    }

    companion object {
        const val REQUEST_KEY = "aimong_gear_sheet"
        const val EXTRA_REFRESH_HOME = "refresh_home"

        fun newInstance(): GearBottomSheet = GearBottomSheet()
    }
}
