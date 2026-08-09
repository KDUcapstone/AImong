package com.kduniv.aimong.feature.settings.presentation

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentNotificationSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationSettingsFragment :
    BaseFragment<FragmentNotificationSettingsBinding>(FragmentNotificationSettingsBinding::inflate) {

    private val viewModel: NotificationSettingsViewModel by viewModels()

    private var restoredStatusBarColor: Int? = null
    private var restoredLightStatusBars: Boolean? = null
    private var suppressAutoSave = false

    private val autoSaveListener = CompoundButton.OnCheckedChangeListener { _, _ ->
        if (suppressAutoSave || !viewModel.canEdit.value) return@OnCheckedChangeListener
        persistCurrentSettings()
    }

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
        binding.btnSave.isVisible = false
        binding.progress.visibility = View.VISIBLE
        installAutoSaveListeners()
        viewModel.load()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isParentRole.collect { isParent ->
                        binding.switchPrivacy.isVisible = isParent
                        binding.tvChildReadonlyHint.isVisible = !isParent
                        binding.tvFcmGatingHint.setText(
                            if (isParent) {
                                R.string.notification_settings_fcm_gating_hint
                            } else {
                                R.string.notification_settings_fcm_gating_hint_child
                            },
                        )
                        if (!isParent) {
                            binding.tvChildReadonlyHint.text =
                                getString(R.string.notification_settings_child_hint)
                        }
                    }
                }
                launch {
                    viewModel.canEdit.collect { editable ->
                        editableSwitches().forEach { it.isEnabled = editable }
                    }
                }
                launch {
                    viewModel.settings.collect { s ->
                        if (s == null) return@collect
                        suppressAutoSave = true
                        binding.switchPrivacy.isChecked = s.privacyAlertEnabled
                        binding.switchStudy.isChecked = s.studyReminderEnabled
                        binding.switchReturnReward.isChecked = s.returnRewardEnabled
                        binding.switchQuestReward.isChecked = s.questRewardEnabled
                        binding.switchMarketing.isChecked = s.marketingEnabled
                        suppressAutoSave = false
                        binding.progress.visibility = View.GONE
                    }
                }
                launch {
                    combine(viewModel.isSaving, viewModel.settings) { saving, settings ->
                        saving to (settings != null)
                    }.collect { (saving, loaded) ->
                        binding.progress.isVisible = saving || !loaded
                    }
                }
                launch {
                    viewModel.messageEvent.collect { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun installAutoSaveListeners() {
        editableSwitches().forEach { it.setOnCheckedChangeListener(autoSaveListener) }
    }

    private fun editableSwitches(): List<SwitchMaterial> =
        if (viewModel.isParentRole.value) {
            listOf(
                binding.switchPrivacy,
                binding.switchStudy,
                binding.switchReturnReward,
                binding.switchQuestReward,
                binding.switchMarketing,
            )
        } else {
            listOf(
                binding.switchStudy,
                binding.switchReturnReward,
                binding.switchQuestReward,
                binding.switchMarketing,
            )
        }

    private fun persistCurrentSettings() {
        viewModel.save(buildPatchFromUi())
    }

    private fun buildPatchFromUi(): NotificationSettingsPatchRequest =
        if (viewModel.isParentRole.value) {
            NotificationSettingsPatchRequest(
                privacyAlertEnabled = binding.switchPrivacy.isChecked,
                studyReminderEnabled = binding.switchStudy.isChecked,
                returnRewardEnabled = binding.switchReturnReward.isChecked,
                questRewardEnabled = binding.switchQuestReward.isChecked,
                marketingEnabled = binding.switchMarketing.isChecked,
            )
        } else {
            NotificationSettingsPatchRequest(
                privacyAlertEnabled = null,
                studyReminderEnabled = binding.switchStudy.isChecked,
                returnRewardEnabled = binding.switchReturnReward.isChecked,
                questRewardEnabled = binding.switchQuestReward.isChecked,
                marketingEnabled = binding.switchMarketing.isChecked,
            )
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
}
