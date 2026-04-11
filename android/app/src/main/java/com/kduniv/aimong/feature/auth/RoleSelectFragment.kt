package com.kduniv.aimong.feature.auth

import androidx.lifecycle.lifecycleScope
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        binding.btnSelectChild.setOnClickListener {
            saveRoleAndRestart("CHILD")
        }
        binding.btnSelectParent.setOnClickListener {
            saveRoleAndRestart("PARENT")
        }
    }

    override fun initObserver() {}

    private fun saveRoleAndRestart(role: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession(role, 1, "temp_token") // 실제 구현시 토큰은 로그인 후 저장
            // 액티비티 재시작하여 Navigation 다시 설정
            activity?.recreate()
        }
    }
}
