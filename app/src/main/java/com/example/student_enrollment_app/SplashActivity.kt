package com.example.student_enrollment_app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDuration = 2000L // 2 seconds
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide brand until animation
        binding.llCenterBrand.isVisible = false

        // Start animation
        startSplashAnimation()

        // Subscribe to FCM topic
        subscribeToFCMTopic()

        // Log FCM token (optional)
        logFCMToken()

        // Delay and check user session
        coroutineScope.launch {
            delay(splashDuration)
            checkUserSession()
        }
    }

    private fun setupEdgeToEdge() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#F2FFFFFF")
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        }
    }

    private fun startSplashAnimation() {
        binding.llCenterBrand.isVisible = true

        // Inside startSplashAnimation()
        val loadingAnim = AnimationUtils.loadAnimation(this, R.anim.loading_text_anim)
        binding.tvLoading.startAnimation(loadingAnim)


        try {
            // Logo animation
            val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_anim)
            binding.ivAppLogo.startAnimation(logoAnim)

            // App name fade + slide animation
            binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(500)
                .start()

            // Loading text fade in
            binding.tvLoading.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(1200)
                .start()

            // Fade in whole brand container
            binding.llCenterBrand.animate()
                .alpha(1f)
                .setDuration(1000)
                .start()

        } catch (e: Exception) {
            Log.e("SplashActivity", "Animation failed", e)
            // fallback simple fade in
            binding.llCenterBrand.alpha = 1f
            binding.tvAppName.alpha = 1f
            binding.tvLoading.alpha = 1f
        }
    }


    private fun subscribeToFCMTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to all_users topic successfully")
                } else {
                    Log.e("FCM", "Failed to subscribe to topic", task.exception)
                }
            }
    }

    private fun logFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token: ${task.result}")
            } else {
                Log.e("FCM_TOKEN", "Failed to get token", task.exception)
            }
        }
    }

    private fun checkUserSession() {
        val user = FirebaseAuth.getInstance().currentUser
        val nextActivity = if (user != null) HomeActivity::class.java else SignInActivity::class.java

        // Optional: fade-out transition for smooth navigation
        binding.root.animate().alpha(0f).setDuration(300).withEndAction {
            startActivity(Intent(this, nextActivity))
            finish()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Prevent memory leaks
    }
}
