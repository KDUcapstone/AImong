package com.kduniv.aimong.feature.auth.presentation

import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setGradientText
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentParentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.feature.auth.domain.RegisterParentFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Color
import javax.inject.Inject

@AndroidEntryPoint
class ParentLoginFragment : BaseFragment<FragmentParentLoginBinding>(FragmentParentLoginBinding::inflate) {

    @Inject
    lateinit var registerParentFcmTokenUseCase: RegisterParentFcmTokenUseCase

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Snackbar.make(binding.root, R.string.auth_google_token_missing, Snackbar.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                signInToFirebase(idToken)
            } catch (e: ApiException) {
                val msg = e.message?.takeIf { it.isNotBlank() } ?: getString(R.string.auth_google_failed)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun initView() {
        binding.tvLoginTitle.setGradientText(
            Color.parseColor("#448AFF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#A040FF")
        )

        binding.btnBack.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().popBackStack(R.id.roleSelectFragment, false)
            }
        }

        binding.btnGoogleLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener { beginGoogleSignIn() }
        }
    }

    private fun googleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun beginGoogleSignIn() {
        googleLauncher.launch(googleSignInClient().signInIntent)
    }

    private fun signInToFirebase(idToken: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
                registerParentFcmTokenUseCase(requireParentSession = false)
                findNavController().navigate(R.id.action_parentLoginFragment_to_parentOnboardingFragment)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    e.message ?: getString(R.string.auth_firebase_failed),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun initObserver() {}
}
