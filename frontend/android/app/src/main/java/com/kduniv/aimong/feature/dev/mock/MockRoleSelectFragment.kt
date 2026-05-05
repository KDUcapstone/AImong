package com.kduniv.aimong.feature.dev.mock

import android.graphics.Color
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding

/** [RoleSelectFragment]와 동일 레이아웃·동선(연동 없음). */
class MockRoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    override fun initView() {
        binding.tvAppNameLogo.setGradientText(
            Color.parseColor("#448AFF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#A040FF")
        )

        binding.btnSelectChild.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(R.id.action_roleSelectFragment_to_childCodeFragment)
            }
        }
        binding.btnSelectParent.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(R.id.action_roleSelectFragment_to_parentLoginFragment)
            }
        }
    }

    override fun initObserver() {}
}
