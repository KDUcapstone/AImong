package com.kduniv.aimong

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigation()
    }

    private fun setupNavigation() {
        lifecycleScope.launch {
            val userRole = sessionManager.userRole.first()

            if (userRole == null) {
                // 역할이 선택되지 않은 경우 RoleSelectFragment가 포함된 메인 그래프 설정
                navController.setGraph(R.navigation.nav_main)
                binding.bottomNav.visibility = View.GONE
            } else {
                // 역할에 따른 전용 그래프 설정
                val navGraph = navController.navInflater.inflate(
                    if (userRole == "CHILD") R.navigation.nav_child else R.navigation.nav_parent
                )
                navController.graph = navGraph

                if (userRole == "CHILD") {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.setupWithNavController(navController)
                    
                    // 네비게이션 아이콘 컬러 문제 해결: 틴트를 null로 설정
                    binding.bottomNav.itemIconTintList = null
                    
                    // [추가] 선택된 아이콘 확대 애니메이션 (어느 탭인지 명확히 표시)
                    binding.bottomNav.setOnItemSelectedListener { item ->
                        // NavController와 연결 유지
                        val navigated = NavigationUI.onNavDestinationSelected(item, navController)
                        
                        // 모든 아이콘 크기 초기화 및 선택된 것만 확대
                        for (i in 0 until binding.bottomNav.menu.size()) {
                            val view = binding.bottomNav.findViewById<View>(binding.bottomNav.menu.getItem(i).itemId)
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                        }
                        val selectedView = binding.bottomNav.findViewById<View>(item.itemId)
                        selectedView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                        
                        navigated
                    }

                    // [추가] 이미 선택된 탭을 다시 눌렀을 때 (예: 하위 화면에서 홈 탭 클릭 시 홈 루트로 복귀)
                    binding.bottomNav.setOnItemReselectedListener { item ->
                        navController.popBackStack(item.itemId, false)
                    }
                } else {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }
}
