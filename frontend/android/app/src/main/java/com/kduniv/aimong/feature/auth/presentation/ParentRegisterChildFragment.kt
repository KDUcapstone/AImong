package com.kduniv.aimong.feature.auth.presentation

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.parent.domain.SyncParentChildrenUseCase
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentRegisterChildBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ParentRegisterChildFragment :
    BaseFragment<FragmentParentRegisterChildBinding>(FragmentParentRegisterChildBinding::inflate) {

    private val viewModel: ParentRegisterViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var syncParentChildrenUseCase: SyncParentChildrenUseCase

    override fun initView() {
        binding.btnBack.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().popBackStack()
        }

        binding.btnSubmit.apply {
            setOnScaleTouchListener()
            setOnClickListener { submit() }
        }
    }

    private fun submit() {
        val raw = binding.etNickname.text?.toString().orEmpty()
        val nickname = raw.trim()
        when {
            nickname.isEmpty() -> {
                Snackbar.make(binding.root, R.string.auth_error_nickname_empty, Snackbar.LENGTH_SHORT).show()
            }
            nickname.length > 20 -> {
                Snackbar.make(binding.root, R.string.auth_error_nickname_length, Snackbar.LENGTH_SHORT).show()
            }
            else -> {
                binding.progress.visibility = android.view.View.VISIBLE
                binding.btnSubmit.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.registerChildProfile(nickname).fold(
                        onSuccess = { data ->
                            binding.progress.visibility = android.view.View.GONE
                            binding.btnSubmit.isEnabled = true
                            showSuccessDialog(data)
                        },
                        onFailure = { e ->
                            binding.progress.visibility = android.view.View.GONE
                            binding.btnSubmit.isEnabled = true
                            Snackbar.make(
                                binding.root,
                                e.message ?: getString(R.string.auth_register_failed),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }
    }

    private fun showSuccessDialog(data: ParentRegisterResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_register_success_title)
            .setMessage(
                getString(
                    R.string.auth_register_success_message,
                    data.nickname,
                    data.code,
                    data.starterTickets
                )
            )
            .setPositiveButton(R.string.auth_confirm) { _, _ ->
                navigateParentHome()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateParentHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession("PARENT", 1, "")
            syncParentChildrenUseCase()
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
