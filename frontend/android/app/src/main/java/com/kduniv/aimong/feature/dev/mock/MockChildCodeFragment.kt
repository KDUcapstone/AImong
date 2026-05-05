package com.kduniv.aimong.feature.dev.mock

import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentChildCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [ChildCodeFragment]와 동일 레이아웃 — 서버 로그인 없이 세션만 저장(목업). */
@AndroidEntryPoint
class MockChildCodeFragment : BaseFragment<FragmentChildCodeBinding>(FragmentChildCodeBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        binding.tvCodeTitle.setGradientText(
            Color.parseColor("#448AFF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#A040FF")
        )

        binding.btnBack.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().popBackStack(R.id.roleSelectFragment, false)
            }
        }

        binding.btnLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener { login() }
        }
    }

    private fun login() {
        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        if (code.length != 6 || code.any { !it.isDigit() }) {
            Snackbar.make(binding.root, R.string.auth_child_code_invalid, Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.btnLogin.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession("CHILD", 1, MockChildSession.TOKEN)
            binding.btnLogin.isEnabled = true
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
