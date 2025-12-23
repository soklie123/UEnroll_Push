package com.example.student_enrollment_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.auth.SignInActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Direct navigation - Remove Firebase check temporarily
        Handler(Looper.getMainLooper()).postDelayed({
            goToSignIn()
        }, 2000)
    }

    private fun goToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish() // Remove splash from back stack
    }
}