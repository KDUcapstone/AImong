package com.kduniv.aimong

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.auth.domain.RegisterChildFcmTokenUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterParentFcmTokenUseCase
import com.kduniv.aimong.feature.parent.domain.SyncParentChildrenUseCase
import com.kduniv.aimong.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        /** 로그인·세션 저장 직후 `MainActivity`를 다시 띄울 때 넣는 플래그(백스택·그래프 초기화). */
        const val EXTRA_IS_RESTART = "IS_RESTART"
    }

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var registerParentFcmTokenUseCase: RegisterParentFcmTokenUseCase

    @Inject
    lateinit var registerChildFcmTokenUseCase: RegisterChildFcmTokenUseCase

    @Inject
    lateinit var syncParentChildrenUseCase: SyncParentChildrenUseCase

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var mainBackPressedCallback: OnBackPressedCallback? = null
    private var exitConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment != null) {
            navController = navHostFragment.navController
            setupNavigation(savedInstanceState)
        }
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        if (!::navController.isInitialized) return
        lifecycleScope.launch {
            val userRole: String? = sessionManager.userRole.first()

            val targetGraphRes = when (userRole) {
                "CHILD" ->
                    if (UiMode.useStubNav) R.navigation.nav_child_stub
                    else R.navigation.nav_child
                "PARENT" ->
                    if (UiMode.useStubNav) R.navigation.nav_parent_stub
                    else R.navigation.nav_parent
                else ->
                    if (UiMode.useStubNav) R.navigation.nav_main_stub
                    else R.navigation.nav_main
            }

            val targetGraphId = when (userRole) {
                "CHILD" ->
                    if (UiMode.useStubNav) R.id.nav_child_stub
                    else R.id.nav_child
                "PARENT" ->
                    if (UiMode.useStubNav) R.id.nav_parent_stub
                    else R.id.nav_parent
                else ->
                    if (UiMode.useStubNav) R.id.nav_main_stub
                    else R.id.nav_main
            }

            // [수정] navController.graph에 직접 접근 시 그래프가 없으면 IllegalStateException이 발생하므로 안전하게 처리
            val currentGraphId = try {
                navController.graph.id
            } catch (e: Exception) {
                null
            }

            val forceGraphFromSessionRestart =
                savedInstanceState == null && intent.getBooleanExtra(EXTRA_IS_RESTART, false)
            if (forceGraphFromSessionRestart || currentGraphId != targetGraphId) {
                navController.setGraph(targetGraphRes)
            }
            if (forceGraphFromSessionRestart) {
                intent.removeExtra(EXTRA_IS_RESTART)
            }

            // 목업 모드(useStubNav)일 때는 서버 통신 UseCase를 호출하지 않음
            if (!UiMode.useStubNav) {
                if (userRole == "PARENT") {
                    registerParentFcmTokenUseCase(requireParentSession = true)
                    syncParentChildrenUseCase()
                }

                if (userRole == "CHILD") {
                    registerChildFcmTokenUseCase(requireChildSession = true)
                }
            }

            if (userRole == "CHILD") {
                binding.bottomNav.visibility = View.VISIBLE
                binding.bottomNav.setupWithNavController(navController)
                binding.bottomNav.itemIconTintList = null
                
                binding.bottomNav.setOnItemSelectedListener { item ->
                    val navigated = NavigationUI.onNavDestinationSelected(item, navController)
                    if (navigated) {
                        // 바텀바 아이템 클릭 시 바운시 효과
                        for (i in 0 until binding.bottomNav.menu.size()) {
                            val menuItem = binding.bottomNav.menu.getItem(i)
                            val view = binding.bottomNav.findViewById<View>(menuItem.itemId)
                            if (view != null) {
                                if (menuItem.itemId == item.itemId) {
                                    view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                                } else {
                                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                                }
                            }
                        }
                    }
                    navigated
                }

                binding.bottomNav.setOnItemReselectedListener { item ->
                    navController.popBackStack(item.itemId, false)
                }
            } else {
                binding.bottomNav.visibility = View.GONE
            }

            installMainBackNavigation(userRole)
        }
    }

    private fun installMainBackNavigation(userRole: String?) {
        val topLevelDestinations = when (userRole) {
            "CHILD" -> setOf(
                R.id.homeFragment,
                R.id.learningFragment,
                R.id.chatFragment,
                R.id.gachaFragment,
                R.id.myProfileFragment,
            )
            "PARENT" -> setOf(R.id.parentDashboardFragment)
            else -> setOf(R.id.roleSelectFragment)
        }
        val appBarConfig = AppBarConfiguration(topLevelDestinations)
        mainBackPressedCallback?.remove()
        mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (NavigationUI.navigateUp(navController, appBarConfig)) return
                showExitConfirmDialog()
            }
        }.also { onBackPressedDispatcher.addCallback(this, it) }
    }

    private fun showExitConfirmDialog() {
        exitConfirmDialog?.dismiss()
        exitConfirmDialog = AlertDialog.Builder(this)
            .setTitle(R.string.app_exit_confirm_title)
            .setMessage(R.string.app_exit_confirm_message)
            .setPositiveButton(R.string.app_exit_confirm_positive) { _, _ -> finish() }
            .setNegativeButton(R.string.app_exit_confirm_negative) { d, _ -> d.dismiss() }
            .setOnDismissListener { exitConfirmDialog = null }
            .show()
    }

    override fun onDestroy() {
        exitConfirmDialog?.dismiss()
        exitConfirmDialog = null
        super.onDestroy()
    }
}
