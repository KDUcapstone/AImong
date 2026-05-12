package com.kduniv.aimong.feature.home.presentation

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentChildMyProfilePlaceholderBinding
import com.kduniv.aimong.feature.auth.domain.LogoutChildUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** MY 탭 — 자녀 로그아웃 등 (실제·목업 동일 destination). */
@AndroidEntryPoint
class ChildMyProfilePlaceholderFragment :
    BaseFragment<FragmentChildMyProfilePlaceholderBinding>(FragmentChildMyProfilePlaceholderBinding::inflate) {

    @Inject
    lateinit var logoutChildUseCase: LogoutChildUseCase

    override fun initView() {
        binding.btnChildLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.child_logout_confirm)
                .setPositiveButton(R.string.child_logout) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        logoutChildUseCase()
                        startActivity(
                            Intent(requireContext(), MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                putExtra(MainActivity.EXTRA_IS_RESTART, true)
                            }
                        )
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun initObserver() {}
}
