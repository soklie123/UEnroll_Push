package com.example.student_enrollment_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.auth.SignInActivity
import com.google.firebase.auth.FirebaseAuth

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the current user from Firebase
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Decide where to go
        if (currentUser == null) {
            // No user is signed in, go to SignInActivity
            startActivity(Intent(this, SignInActivity::class.java))
        } else {
            // User is already signed in, go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // IMPORTANT: Finish this LaunchActivity so the user can't navigate back to it.
        finish()
    }
}
