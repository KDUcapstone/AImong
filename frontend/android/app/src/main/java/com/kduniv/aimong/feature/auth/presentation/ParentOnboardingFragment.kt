package com.kduniv.aimong.feature.auth.presentation

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ParentOnboardingFragment :
    BaseFragment<FragmentParentOnboardingBinding>(FragmentParentOnboardingBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        binding.btnBack.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().popBackStack()
        }

        binding.btnViewChildren.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                navigateToDashboard()
            }
        }

        binding.btnRegisterChild.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().navigate(R.id.action_parentOnboardingFragment_to_parentRegisterChildFragment)
            }
        }
    }

    private fun navigateToDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.saveSession("PARENT", 1, "")
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
