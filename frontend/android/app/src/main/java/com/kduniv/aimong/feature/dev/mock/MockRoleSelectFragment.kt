package com.kduniv.aimong.feature.dev.mock

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding

/** [RoleSelectFragment]와 동일 레이아웃·동선(연동 없음). */
class MockRoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    override fun initView() {
        binding.btnSelectChild.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                bounceClick(this) {
                    findNavController().navigate(R.id.action_roleSelectFragment_to_childCodeFragment)
                }
            }
        }
        binding.btnSelectParent.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                bounceClick(this) {
                    findNavController().navigate(R.id.action_roleSelectFragment_to_parentLoginFragment)
                }
            }
        }
    }

    private fun bounceClick(view: View, onClick: () -> Unit) {
        view.isEnabled = false
        view.post {
            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f
        }
        view.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(80)
            .withEndAction {
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
