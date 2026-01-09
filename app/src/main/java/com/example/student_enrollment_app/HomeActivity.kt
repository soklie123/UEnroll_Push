package com.example.student_enrollment_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivityHomeBinding
import com.example.student_enrollment_app.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "HomeActivity"
    }

    // Permission launcher for Android 13+
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("NotificationPermission", "Permission granted")
                getFCMToken()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied. You may miss updates.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        setupNavigation()
        setupTopBarListeners()
        listenUnreadNotifications()
        requestNotificationPermission()
        handleNotificationIntent(intent)

        // Load user profile picture
        loadUserProfilePicture()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile picture when activity resumes
        loadUserProfilePicture()
    }

    // -------------------- PROFILE PICTURE --------------------
    private fun loadUserProfilePicture() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No user logged in")
            return
        }

        Log.d(TAG, "Loading profile picture for user: ${currentUser.uid}")

        // Fetch user data from Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        // Get profile image URL from Firestore
                        val profileImageUrl = document.getString("profileImageUrl")

                        Log.d(TAG, "Profile image URL: $profileImageUrl")

                        // Load image with Glide
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(binding.iconProfile)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading profile picture: ${e.message}", e)
                        loadFallbackProfilePicture()
                    }
                } else {
                    Log.w(TAG, "User document not found")
                    loadFallbackProfilePicture()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user data: ${e.message}", e)
                loadFallbackProfilePicture()
            }
    }

    private fun loadFallbackProfilePicture() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Try to load from Firebase Auth profile
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(binding.iconProfile)
        } else {
            // Load placeholder
            binding.iconProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    // -------------------- BADGE --------------------
    private fun updateBadge(unreadCount: Int) {
        binding.badgeDot.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
    }

    private fun listenUnreadNotifications() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val unreadCount = snapshot.size()
                updateBadge(unreadCount)
            }
    }

    // -------------------- UI --------------------
    private fun setupEdgeToEdge() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = true
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in listOf(
                R.id.homeScreenFragment,
                R.id.statusScreenFragment,
                R.id.notificationScreenFragment,
                R.id.profileScreenFragment
            )

            binding.customTopBar.visibility = if (isTopLevel) View.VISIBLE else View.GONE
            binding.bottomNav.visibility = if (isTopLevel) View.VISIBLE else View.GONE
        }
    }

    private fun setupTopBarListeners() {
        binding.iconNotification.setOnClickListener {
            binding.badgeDot.visibility = View.GONE // hide badge
            binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
        }

        binding.iconProfile.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.profileScreenFragment
        }
    }

    // -------------------- AUTH & FCM --------------------
    private fun redirectToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> getFCMToken()

                shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    Toast.makeText(
                        this,
                        "Notification permission is required to receive updates.",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }

                else -> requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching token failed", task.exception)
                return@addOnCompleteListener
            }
            sendTokenToFirestore(task.result)
        }
    }

    private fun sendTokenToFirestore(token: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .set(
                hashMapOf(
                    "fcmToken" to token,
                    "updatedAt" to Timestamp.now()
                ),
                SetOptions.merge()
            )
    }

    // -------------------- INTENT --------------------
    private fun handleNotificationIntent(intent: Intent?) {
        when (intent?.getStringExtra("openFragment")) {
            "notifications" -> binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
            "status" -> binding.bottomNav.selectedItemId = R.id.homeScreenFragment
            "profile" -> binding.bottomNav.selectedItemId = R.id.profileScreenFragment
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}