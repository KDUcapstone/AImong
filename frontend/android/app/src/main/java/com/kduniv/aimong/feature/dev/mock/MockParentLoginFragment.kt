package com.kduniv.aimong.feature.dev.mock

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [ParentLoginFragment]와 동일 레이아웃 — Google 계정 선택 없이 부모 세션 후 자녀 등록 화면으로 이동(목업, mock `/parent-onboarding`과 동일). */
@AndroidEntryPoint
class MockParentLoginFragment : BaseFragment<FragmentParentLoginBinding>(FragmentParentLoginBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        binding.btnBack.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().popBackStack(R.id.roleSelectFragment, false)
            }
        }

        binding.btnGoogleLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    sessionManager.saveSession("PARENT", 1, "")
                    findNavController().navigate(R.id.action_parentLoginFragment_to_parentRegisterChildFragment)
                    Snackbar.make(binding.root, R.string.auth_mock_google_instant, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initObserver() {}
}
