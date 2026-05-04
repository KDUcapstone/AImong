package com.kduniv.aimong

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // [핵심] '재시작' 플래그가 없는 경우(앱을 처음 켰을 때) 세션을 초기화하거나 유지합니다.
        val isRestart = intent.getBooleanExtra("IS_RESTART", false)
        
        if (savedInstanceState == null && !isRestart) {
            lifecycleScope.launch {
                // 앱 최초 실행 시 세션 클리어 (필요한 경우에만)
                // sessionManager.clearSession() 
                setupNavigation()
            }
        } else {
            // 복원 시에는 이미 그래프가 설정되어 있을 수 있으므로 주의
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        lifecycleScope.launch {
            // 현재 저장된 유저 역할을 가져옴
            val userRole = sessionManager.userRole.first()

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

            if (currentGraphId != targetGraphId) {
                navController.setGraph(targetGraphRes)
            }

            if (userRole == "PARENT") {
                registerParentFcmTokenUseCase(requireParentSession = true)
                syncParentChildrenUseCase()
            }

            if (userRole == "CHILD") {
                registerChildFcmTokenUseCase(requireChildSession = true)
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
        }
    }
}
