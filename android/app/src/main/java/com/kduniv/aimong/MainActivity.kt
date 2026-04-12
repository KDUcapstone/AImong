package com.kduniv.aimong

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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
                } else {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }
}
