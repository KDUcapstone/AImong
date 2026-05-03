package com.kduniv.aimong.feature.auth

import android.graphics.Color
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    override fun initView() {
        // AI 펫 로고 아래 텍스트에 그라데이션 적용
        binding.tvAppNameLogo.setGradientText(
            Color.parseColor("#448AFF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#A040FF")
        )

        binding.btnSelectChild.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(com.kduniv.aimong.R.id.action_roleSelectFragment_to_childCodeFragment)
            }
        }
        binding.btnSelectParent.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(com.kduniv.aimong.R.id.action_roleSelectFragment_to_parentLoginFragment)
            }
        }
    }

    override fun initObserver() {}
}
