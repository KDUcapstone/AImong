package com.kduniv.aimong.feature.auth.presentation

import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding // 임시 바인딩 (Splash 전용 레이아웃 부재 시)

class SplashFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {
    override fun initView() {
        // TODO: 스플래시 UI 및 자동 로그인 로직
    }

    override fun initObserver() {
        // TODO: 관찰자 초기화
    }
}
