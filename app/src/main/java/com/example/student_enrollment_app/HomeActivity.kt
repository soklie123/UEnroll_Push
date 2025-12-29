package com.example.student_enrollment_app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        setupNavigation()
        setupTopBarListeners()
    }

    private fun setupEdgeToEdge() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }

    private fun redirectToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Attach BottomNavigation to NavController
        binding.bottomNav.setupWithNavController(navController)

        // Hide/show TopBar & BottomNav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.departmentDetailFragment -> {
                    binding.customTopBar.visibility = View.GONE
                    binding.bottomNav.visibility = View.GONE
                }
                R.id.homeScreenFragment,
                R.id.notificationScreenFragment,
                R.id.profileScreenFragment -> {
                    binding.customTopBar.visibility = View.VISIBLE
                    binding.bottomNav.visibility = View.VISIBLE
                }
                else -> {
                    binding.customTopBar.visibility = View.VISIBLE
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupTopBarListeners() {
        // Safe navigation: only change selected BottomNav item
        binding.iconNotification.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
        }

        binding.iconProfile.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.profileScreenFragment
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
