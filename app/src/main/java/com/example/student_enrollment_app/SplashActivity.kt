package com.example.student_enrollment_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Binding
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Start Animation immediately
        try {
            val animation = AnimationUtils.loadAnimation(this, R.anim.splash_anim)
            binding.llCenterBrand.startAnimation(animation)
        } catch (e: Exception) {
            // Fallback if animation file is missing
        }

        // 2. Delay 2 seconds then transition
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 2000)
    }

    private fun checkUserSession() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            // Go to Home if already logged in
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            // Go to Login if first time
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}