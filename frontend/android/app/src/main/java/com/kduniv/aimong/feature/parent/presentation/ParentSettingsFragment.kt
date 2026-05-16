package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParentSettingsFragment :
    BaseFragment<FragmentParentSettingsBinding>(FragmentParentSettingsBinding::inflate) {

    private val viewModel: ParentSettingsViewModel by viewModels()

    override fun initView() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnNotificationSettings.setOnClickListener {
            val nav = findNavController()
            val navigated = runCatching {
                nav.navigate(R.id.action_parentSettingsFragment_to_notificationSettingsFragment)
            }.isSuccess
            if (!navigated) {
                runCatching { nav.navigate(R.id.notificationSettingsFragment) }
            }
        }
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.parent_logout_confirm)
                .setPositiveButton(R.string.parent_logout) { _, _ -> viewModel.logout() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.parent_delete_account)
                .setMessage(R.string.parent_delete_account_confirm)
                .setPositiveButton(R.string.parent_delete_account) { _, _ -> viewModel.deleteAccount() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        viewModel.load()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.parentMe.collect { me ->
                        binding.tvAccountSummary.text = if (me == null) {
                            getString(R.string.parent_settings_account_loading)
                        } else {
                            getString(
                                R.string.parent_settings_account_fmt,
                                me.email ?: "—",
                                me.childrenCount ?: 0
                            )
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.messageEvent.collect { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.navigateToLogin.collect {
                        startActivity(
                            Intent(requireContext(), MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                putExtra(MainActivity.EXTRA_IS_RESTART, true)
                            }
                        )
                    }
                }
            }
        }
    }
}
