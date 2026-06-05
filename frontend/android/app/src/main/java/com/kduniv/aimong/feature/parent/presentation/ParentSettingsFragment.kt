package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.kduniv.aimong.databinding.ItemParentSettingsMenuRowBinding
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

        setupMenuRow(
            binding = ItemParentSettingsMenuRowBinding.bind(binding.rowNotificationSettings.root),
            iconRes = R.drawable.ic_notifications,
            iconBgRes = R.drawable.bg_parent_settings_icon_blue,
            iconTint = R.color.parent_mock_blue,
            label = getString(R.string.notification_settings_title),
        ) {
            val nav = findNavController()
            val navigated = runCatching {
                nav.navigate(R.id.action_parentSettingsFragment_to_notificationSettingsFragment)
            }.isSuccess
            if (!navigated) {
                runCatching { nav.navigate(R.id.notificationSettingsFragment) }
            }
        }

        setupMenuRow(
            binding = ItemParentSettingsMenuRowBinding.bind(binding.rowLogout.root),
            iconRes = R.drawable.ic_exit_logout,
            iconBgRes = R.drawable.bg_parent_settings_icon_red,
            iconTint = R.color.parent_mock_logout,
            label = getString(R.string.parent_logout),
        ) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.parent_logout_confirm)
                .setPositiveButton(R.string.parent_logout) { _, _ -> viewModel.logout() }
                .setNegativeButton(R.string.parent_dialog_cancel, null)
                .show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.parent_delete_account)
                .setMessage(R.string.parent_delete_account_confirm)
                .setPositiveButton(R.string.parent_delete_account) { _, _ -> viewModel.deleteAccount() }
                .setNegativeButton(R.string.parent_dialog_cancel, null)
                .show()
        }
        viewModel.load()
    }

    private fun setupMenuRow(
        binding: ItemParentSettingsMenuRowBinding,
        iconRes: Int,
        iconBgRes: Int,
        iconTint: Int,
        label: String,
        onClick: () -> Unit,
    ) {
        binding.frameIcon.setBackgroundResource(iconBgRes)
        binding.ivIcon.setImageResource(iconRes)
        binding.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), iconTint))
        binding.tvLabel.text = label
        binding.root.setOnClickListener { onClick() }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.parentMe.collect { me ->
                        if (me == null) {
                            binding.tvAccountEmail.text =
                                getString(R.string.parent_settings_account_loading)
                            binding.tvAccountChildren.visibility = View.GONE
                        } else {
                            binding.tvAccountEmail.text = me.email ?: "—"
                            binding.tvAccountChildren.visibility = View.VISIBLE
                            binding.tvAccountChildren.text = getString(
                                R.string.parent_settings_children_fmt,
                                me.childrenCount ?: 0,
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
