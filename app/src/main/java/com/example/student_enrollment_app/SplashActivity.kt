package com.example.student_enrollment_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDuration = 2000L // 2 seconds
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide brand until animation
        binding.llCenterBrand.isVisible = false

        // Start animation
        startSplashAnimation()

        // Subscribe to FCM topic
        subscribeToFCMTopic()

        // Optional: Get FCM token for debugging
        logFCMToken()

        // Delay then check session
        coroutineScope.launch {
            delay(splashDuration)
            checkUserSession()
        }
    }

    private fun startSplashAnimation() {
        binding.llCenterBrand.isVisible = true
        try {
            val animation = AnimationUtils.loadAnimation(this, R.anim.splash_anim)
            binding.llCenterBrand.startAnimation(animation)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Animation not found or failed", e)
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
        startActivity(Intent(this, nextActivity))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Prevent memory leaks
    }
}
