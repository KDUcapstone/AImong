package com.kduniv.aimong.feature.auth

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentRoleSelectBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoleSelectFragment : BaseFragment<FragmentRoleSelectBinding>(FragmentRoleSelectBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

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

        binding.btnBypassLogin.setOnClickListener {
            // [테스트용] 바로 홈화면으로 진입 (자녀 역할로 설정)
            saveRoleAndRestart("CHILD")
        }
    }

    private fun saveRoleAndRestart(role: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession(role, 1, "test_token")
            val intent = android.content.Intent(requireContext(), com.kduniv.aimong.MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("IS_RESTART", true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
