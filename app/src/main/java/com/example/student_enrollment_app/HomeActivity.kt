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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivityHomeBinding
import com.example.student_enrollment_app.model.Department
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NotificationPermission", "Permission granted.")
            getFCMToken()
        } else {
            Log.w("NotificationPermission", "Permission denied.")
            Toast.makeText(this, "Notification permission denied. You may miss updates.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Redirect if user not logged in
        if (auth.currentUser == null) {
            redirectToSignIn()
            return
        }

        setupNavigation()
        setupTopBarListeners()
        requestNotificationPermission()
        handleNotificationIntent(intent)

        // Start Firestore listener for new departments
        listenForNewDepartments()
    }

    private fun setupEdgeToEdge() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        }
    }

    private fun redirectToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = when (destination.id) {
                R.id.homeScreenFragment,
                R.id.statusScreenFragment,
                R.id.notificationScreenFragment,
                R.id.profileScreenFragment -> true
                else -> false
            }

            binding.customTopBar.visibility = if (isTopLevel) View.VISIBLE else View.GONE
            binding.bottomNav.visibility = if (isTopLevel) View.VISIBLE else View.GONE
        }
    }

    private fun setupTopBarListeners() {
        binding.iconNotification.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.notificationScreenFragment
        }
        binding.iconProfile.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.profileScreenFragment
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> getFCMToken()
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(this, "Notification permission is required to receive updates.", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            val token = task.result
            Log.d("FCM_TOKEN", "Token: $token")
            sendTokenToFirestore(token)
        }
    }

    private fun sendTokenToFirestore(token: String) {
        val userId = auth.currentUser?.uid ?: return
        val userDocRef = firestore.collection("users").document(userId)
        val data = hashMapOf("fcmToken" to token, "updatedAt" to Timestamp.now())
        userDocRef.set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("FCM_TOKEN", "Saved token to Firestore") }
            .addOnFailureListener { e -> Log.e("FCM_TOKEN", "Error saving token", e) }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val fragmentToOpen = intent?.getStringExtra("openFragment") ?: return
        when (fragmentToOpen) {
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

    // ------------------- Firestore real-time listener -------------------
    private fun listenForNewDepartments() {
        val db = FirebaseFirestore.getInstance()
        val faculties = listOf("engineering", "science", "education") // list your faculty IDs here

        for (facultyId in faculties) {
            db.collection("faculties")
                .document(facultyId)
                .collection("departments")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("FirestoreListener", "Listen failed", error)
                        return@addSnapshotListener
                    }

                    snapshots?.documentChanges?.forEach { docChange ->
                        if (docChange.type == DocumentChange.Type.ADDED) {
                            val newDept = docChange.document.toObject(Department::class.java)
                            Log.d("FirestoreListener", "New department added: ${newDept.name}")

                            showLocalNotification(
                                "New Department Added",
                                "Department ${newDept.name} is now available"
                            )
                        }
                    }
                }
        }
    }

    private fun showLocalNotification(title: String, message: String) {
        val channelId = "admin_updates"
        val notificationId = (System.currentTimeMillis() / 1000).toInt()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Admin Updates", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
