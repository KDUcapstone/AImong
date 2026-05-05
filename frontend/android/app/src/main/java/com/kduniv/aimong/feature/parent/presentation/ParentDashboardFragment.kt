package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ParentDashboardFragment : BaseFragment<FragmentParentDashboardBinding>(FragmentParentDashboardBinding::inflate) {

    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var adapter: ParentChildAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    override fun initView() {
        adapter = ParentChildAdapter { childId ->
            viewModel.regenerateChildCode(childId)
        }

        binding.rvChildren.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChildren.adapter = adapter

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            viewLifecycleOwner.lifecycleScope.launch {
                sessionManager.clearSession()
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(MainActivity.EXTRA_IS_RESTART, true)
                }
                startActivity(intent)
            }
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.children.collect { children ->
                        adapter.submitList(children)
                    }
                }
                launch {
                    viewModel.messageEvent.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

