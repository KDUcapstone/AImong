package com.kduniv.aimong.feature.dev.mock

import android.graphics.Color
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentLoginBinding

/** [ParentLoginFragment]와 동일 레이아웃 — Google 없이 다음 화면으로 이동(목업). */
class MockParentLoginFragment : BaseFragment<FragmentParentLoginBinding>(FragmentParentLoginBinding::inflate) {

    override fun initView() {
        binding.tvLoginTitle.setGradientText(
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

        binding.btnGoogleLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(R.id.action_parentLoginFragment_to_parentOnboardingFragment)
            }
        }
    }

    override fun initObserver() {}
}
