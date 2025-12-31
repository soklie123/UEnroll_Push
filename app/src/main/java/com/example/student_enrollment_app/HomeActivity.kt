package com.example.student_enrollment_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Permission launcher for notification permission (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NotificationPermission", "Permission granted")
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            getFCMToken()
        } else {
            Log.d("NotificationPermission", "Permission denied")
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

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
        firestore = FirebaseFirestore.getInstance()

        // Redirect to SignIn if user not logged in
        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        // Setup bottom navigation with NavController
        setupNavigation()

        // Handle top bar buttons
        setupTopBarListeners()

        // Request notification permission
        requestNotificationPermission()

        // Handle intent from notification click
        handleNotificationIntent(intent)
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d("NotificationPermission", "Permission already granted")
                    getFCMToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation to user and request permission
                    Toast.makeText(
                        this,
                        "Notification permission is needed to receive updates",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, no runtime permission needed
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM_TOKEN", "FCM Token: $token")

            // Save token to Firestore
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        val userId = auth.currentUser?.uid ?: return

        val userTokenData = hashMapOf(
            "fcmToken" to token,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users")
            .document(userId)
            .update(userTokenData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("FCM_TOKEN", "Token saved to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_TOKEN", "Error saving token", e)

                // If update fails (document might not exist), try to set it
                firestore.collection("users")
                    .document(userId)
                    .set(userTokenData)
                    .addOnSuccessListener {
                        Log.d("FCM_TOKEN", "Token created in Firestore successfully")
                    }
                    .addOnFailureListener { error ->
                        Log.e("FCM_TOKEN", "Error creating token document", error)
                    }
            }
    }

    // Handle notifications when activity is opened from a system notification
    private fun handleNotificationIntent(intent: Intent?) {
        val fragmentToOpen = intent?.getStringExtra("openFragment")

        when (fragmentToOpen) {
            "notifications" -> {
                binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
            }
            "status" -> {
                binding.bottomNav.selectedItemId = R.id.homeScreenFragment
                // You can pass additional data to the fragment if needed
                val statusId = intent.getStringExtra("statusId")
                Log.d("NotificationIntent", "Opening status with ID: $statusId")
            }
            "profile" -> {
                binding.bottomNav.selectedItemId = R.id.profileScreenFragment
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle notifications if activity already running
        handleNotificationIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}