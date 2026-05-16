package com.kduniv.aimong.feature.settings.presentation

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentNotificationSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationSettingsFragment :
    BaseFragment<FragmentNotificationSettingsBinding>(FragmentNotificationSettingsBinding::inflate) {

    private val viewModel: NotificationSettingsViewModel by viewModels()

    override fun initView() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnSave.setOnClickListener { saveCurrent() }
        viewModel.load()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

