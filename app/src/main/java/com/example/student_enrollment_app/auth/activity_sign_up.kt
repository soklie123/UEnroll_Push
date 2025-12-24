package com.example.student_enrollment_app.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.HomeActivity
import com.example.student_enrollment_app.databinding.ActivitySignUpBinding
import com.example.student_enrollment_app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // Add this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore // Initialize Firestore

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmailSignUp.text.toString().trim()
            val password = binding.etPasswordSignUp.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(fullName, email, password, confirmPassword)) {
                signUpWithEmail(email, password, fullName)
            }
        }

        binding.tvSignInRedirect.setOnClickListener {
            // Ensure SignInActivity exists in your auth package
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
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailSignUp.error = "Enter a valid email"
            return false
        }
        if (password.length < 6) {
            binding.etPasswordSignUp.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun signUpWithEmail(email: String, pass: String, fullName: String) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    // Use your Model
                    val user = User(
                        uid = uid,
                        fullName = fullName,
                        email = email,
                        profilePic = "",
                        enrolledDepartmentId = null
                    )

                    // Save to Firestore using the initialized 'db'
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            showLoading(false)
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarSignUp.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSignUp.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }
}