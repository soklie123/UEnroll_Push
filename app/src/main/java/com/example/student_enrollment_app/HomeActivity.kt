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

        // Set edge-to-edge display
        setupEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set navigation bar color for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }

        auth = FirebaseAuth.getInstance()

        // 1. Safety Check - Redirect to SignIn if not authenticated
        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        // 2. Setup Navigation
        setupNavigation()

        // 3. Setup Top Bar Listeners
        setupTopBarListeners()
    }

    private fun setupEdgeToEdge() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT

            // Allow content to flow under the status bar
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

        // Sync Bottom Navigation with NavController
        binding.bottomNav.setupWithNavController(navController)

        // Optional: Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // You can update UI based on current destination
            when (destination.id) {
                R.id.homeScreenFragment -> {  // FIXED: Changed from homeScreenFragment to homeFragment
                    // Home fragment is active
                }
                R.id.notificationScreenFragment -> {
                    // Notification fragment is active
                }
                R.id.profileScreenFragment -> {
                    // Profile fragment is active
                }
            }
        }
    }

    private fun setupTopBarListeners() {
        binding.iconNotification.setOnClickListener {
            // Navigate to notification screen
            navController.navigate(R.id.notificationScreenFragment)
        }

        binding.iconProfile.setOnClickListener {
            // Navigate to profile screen
            navController.navigate(R.id.profileScreenFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}