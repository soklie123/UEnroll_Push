package com.example.student_enrollment_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupClickListeners()



    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmailSignUp.text.toString().trim()
            val password = binding.etPasswordSignUp.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(fullName, email, password, confirmPassword)) {
                signUpWithEmail(email, password)
            }
        }

        binding.tvSignInRedirect.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun validateInput(fullName: String, email: String,
                              password: String, confirmPassword: String): Boolean {
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmailSignUp.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailSignUp.error = "Enter a valid email"
            return false
        }
        if (password.isEmpty()) {
            binding.etPasswordSignUp.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.etPasswordSignUp.error = "Password must be at least 6 characters"
            return false
        }
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun signUpWithEmail(email: String, password: String) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarSignUp.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSignUp.isEnabled = !show
        binding.tvSignInRedirect.isEnabled = !show
    }
}