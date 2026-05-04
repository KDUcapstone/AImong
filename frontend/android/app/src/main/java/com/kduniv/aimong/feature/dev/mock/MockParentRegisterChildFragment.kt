package com.kduniv.aimong.feature.dev.mock

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentRegisterChildBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** [ParentRegisterChildFragment]와 동일 레이아웃 — API 없이 성공 플로우만 재현. */
@AndroidEntryPoint
class MockParentRegisterChildFragment :
    BaseFragment<FragmentParentRegisterChildBinding>(FragmentParentRegisterChildBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSubmit.apply {
            setOnScaleTouchListener()
            setOnClickListener { submit() }
        }
    }

    private fun submit() {
        val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()
        when {
            nickname.isEmpty() -> {
                Snackbar.make(binding.root, R.string.auth_error_nickname_empty, Snackbar.LENGTH_SHORT).show()
            }
            nickname.length > 20 -> {
                Snackbar.make(binding.root, R.string.auth_error_nickname_length, Snackbar.LENGTH_SHORT).show()
            }
            else -> {
                val mock = ParentRegisterResponse(
                    childId = UUID.randomUUID().toString(),
                    nickname = nickname,
                    code = "123456",
                    starterTickets = 3
                )
                showSuccessDialog(mock)
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
            .setPositiveButton(R.string.auth_confirm) { _, _ -> navigateParentHome() }
            .setCancelable(false)
            .show()
    }

    private fun navigateParentHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession("PARENT", 1, "")
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
