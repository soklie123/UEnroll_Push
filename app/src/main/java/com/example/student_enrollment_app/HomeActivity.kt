package com.example.student_enrollment_app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set navigation bar color for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }

        auth = FirebaseAuth.getInstance()

        // Redirect to SignIn if user not logged in
        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        // Setup bottom navigation with NavController
        setupNavigation()

        // Handle top bar buttons
        setupTopBarListeners()

        // Handle intent from notification click
        handleNotificationIntent(intent)

        // Get FCM token for this device
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", token ?: "No token found")
                // You can send this token to your server/admin if needed
            } else {
                Log.e("FCM_TOKEN", "Failed to get FCM token", task.exception)
            }
        }
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
        // Navigate to notifications
        binding.iconNotification.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
        }

        // Navigate to profile
        binding.iconProfile.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.profileScreenFragment
        }
    }

    // Handle notifications when activity is opened from a system notification
    private fun handleNotificationIntent(intent: Intent?) {
        val fragmentToOpen = intent?.getStringExtra("openFragment")
        if (fragmentToOpen == "notifications") {
            binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle notifications if activity already running
        handleNotificationIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
