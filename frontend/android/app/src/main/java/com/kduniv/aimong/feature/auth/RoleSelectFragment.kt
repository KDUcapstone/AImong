package com.kduniv.aimong.feature.auth

import android.graphics.Color
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    override fun initView() {
        // 목업 UI에서는 단색 타이틀을 사용한다.

        binding.btnSelectChild.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                bounceClick(this) {
                    findNavController().navigate(com.kduniv.aimong.R.id.action_roleSelectFragment_to_childCodeFragment)
                }
            }
        }
        binding.btnSelectParent.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                bounceClick(this) {
                    findNavController().navigate(com.kduniv.aimong.R.id.action_roleSelectFragment_to_parentLoginFragment)
                }
            }
        }
    }

    private fun bounceClick(view: View, onClick: () -> Unit) {
        view.isEnabled = false
        // 스케일이 가장자리에서 잘리지 않도록 중심 pivot 고정
        view.post {
            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f
        }
        view.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(80)
            .withEndAction {
                // 원복 시 살짝 오버슈트로 "뽀잉" 느낌
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(OvershootInterpolator(1.6f))
                    .setDuration(160)
                    .withEndAction { view.isEnabled = true }
                    .start()
                onClick()
            }
            .start()
    }

    override fun initObserver() {}
}
