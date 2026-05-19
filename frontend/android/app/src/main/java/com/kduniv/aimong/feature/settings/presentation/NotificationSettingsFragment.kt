package com.kduniv.aimong.feature.settings.presentation

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentNotificationSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationSettingsFragment :
    BaseFragment<FragmentNotificationSettingsBinding>(FragmentNotificationSettingsBinding::inflate) {

    private val viewModel: NotificationSettingsViewModel by viewModels()

    private var restoredStatusBarColor: Int? = null
    private var restoredLightStatusBars: Boolean? = null

    override fun shouldApplySystemBarInsets(): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBrightSystemChrome()
    }

    override fun onDestroyView() {
        restoreSystemChrome()
        super.onDestroyView()
    }

    override fun initView() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSave.setOnClickListener { saveCurrent() }
        binding.progress.visibility = View.VISIBLE
        viewModel.load()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isParentRole.collect { isParent ->
                        val accent = ContextCompat.getColor(
                            requireContext(),
                            if (isParent) R.color.parent_mock_blue else R.color.quiz_mint,
                        )
                        binding.btnSave.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(accent)
                    }
                }
                launch {
                    viewModel.canEdit.collect { editable ->
                        binding.btnSave.visibility = if (editable) View.VISIBLE else View.GONE
                        binding.tvChildReadonlyHint.visibility =
                            if (editable) View.GONE else View.VISIBLE
                        val switches = listOf(
                            binding.switchPrivacy,
                            binding.switchStudy,
                            binding.switchReturnReward,
                            binding.switchQuestReward,
                            binding.switchMarketing,
                        )
                        switches.forEach { it.isEnabled = editable }
                    }
                }
                launch {
                    viewModel.settings.collect { s ->
                        if (s == null) return@collect
                        binding.switchPrivacy.isChecked = s.privacyAlertEnabled
                        binding.switchStudy.isChecked = s.studyReminderEnabled
                        binding.switchReturnReward.isChecked = s.returnRewardEnabled
                        binding.switchQuestReward.isChecked = s.questRewardEnabled
                        binding.switchMarketing.isChecked = s.marketingEnabled
                        binding.progress.visibility = View.GONE
                    }
                }
                launch {
                    viewModel.messageEvent.collect { msg ->
                        binding.progress.visibility = View.GONE
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun applyBrightSystemChrome() {
        val window = activity?.window ?: return
        val decor = window.decorView
        val bg = ContextCompat.getColor(requireContext(), R.color.settings_screen_bg)
        if (restoredStatusBarColor == null) {
            restoredStatusBarColor = window.statusBarColor
            restoredLightStatusBars =
                WindowCompat.getInsetsController(window, decor).isAppearanceLightStatusBars
        }
        window.statusBarColor = bg
        WindowCompat.getInsetsController(window, decor).isAppearanceLightStatusBars = true

        val density = resources.displayMetrics.density
        val basePad = (20f * density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                basePad + bars.left,
                basePad + bars.top,
                basePad + bars.right,
                basePad + bars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun restoreSystemChrome() {
        val window = activity?.window ?: return
        val decor = window.decorView
        restoredStatusBarColor?.let { window.statusBarColor = it }
        restoredLightStatusBars?.let {
            WindowCompat.getInsetsController(window, decor).isAppearanceLightStatusBars = it
        }
        restoredStatusBarColor = null
        restoredLightStatusBars = null
    }

    private fun saveCurrent() {
        binding.progress.visibility = View.VISIBLE
        viewModel.save(
            NotificationSettingsPatchRequest(
                privacyAlertEnabled = binding.switchPrivacy.isChecked,
                studyReminderEnabled = binding.switchStudy.isChecked,
                returnRewardEnabled = binding.switchReturnReward.isChecked,
                questRewardEnabled = binding.switchQuestReward.isChecked,
                marketingEnabled = binding.switchMarketing.isChecked
            )
        )
    }
}
