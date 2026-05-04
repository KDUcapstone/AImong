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
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment != null) {
            navController = navHostFragment.navController
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        if (!::navController.isInitialized) return
        lifecycleScope.launch {
            // [임시 추가] 앱 시작 시 항상 로그인 화면을 보기 위해 세션 초기화
            // sessionManager.clearSession()
            
            // 디버깅/목업 확인을 위해 항상 null(초기 상태)로 인식하게 임시 처리
            val userRole: String? = null // sessionManager.userRole.first()

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
        }
    }
}
