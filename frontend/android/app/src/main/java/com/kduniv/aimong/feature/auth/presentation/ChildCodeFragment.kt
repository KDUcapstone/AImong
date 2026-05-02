package com.kduniv.aimong.feature.auth.presentation

import android.graphics.Color
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentChildCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChildCodeFragment : BaseFragment<FragmentChildCodeBinding>(FragmentChildCodeBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        // 남색에서 보라색으로 이어지는 3색 그라데이션 적용
        binding.tvCodeTitle.setGradientText(
            Color.parseColor("#448AFF"), // Navy-Blue 시작
            Color.parseColor("#7C4DFF"), // Purple 중간
            Color.parseColor("#A040FF")  // Light Purple 끝
        )

        binding.btnBack.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                // 목적지를 명시하여 해당 화면이 나올 때까지만 스택을 제거 (앱 종료 방지)
                findNavController().popBackStack(com.kduniv.aimong.R.id.roleSelectFragment, false)
            }
        }

        binding.btnLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                val code = binding.etCode.text.toString()
                if (code.length == 6) {
                    // [변경] 코드가 맞으면 닉네임 설정 화면으로 이동 (자녀가 직접 설정)
                    findNavController().navigate(com.kduniv.aimong.R.id.action_childCodeFragment_to_childNicknameFragment)
                }
            }
        }
    }

    override fun initObserver() {}
}
