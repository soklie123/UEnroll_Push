package com.example.student_enrollment_app.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.student_enrollment_app.HomeActivity
import com.example.student_enrollment_app.databinding.ActivitySignInBinding
import com.example.student_enrollment_app.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_SIGN_IN = 123

    companion object {
        private const val TAG = "SignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToHome()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("799233073197-3f9dc7etp2lce5i5nl7rqsqk1v5ioojh.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile() // ✅ Important: Request profile to get displayName
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnSignInGoogle.setOnClickListener {
            showLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmailSignIn.text.toString().trim()
            val password = binding.etPasswordSignIn.text.toString().trim()

            if (validateInputs(email, password)) {
                signInUser(email, password)
            }
        }

        binding.tvSignUpRedirect.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailSignIn.error = "A valid email is required"
            return false
        }
        if (password.length < 6) {
            binding.etPasswordSignIn.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun signInUser(email: String, password: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sign-in successful")
                    navigateToHome()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Log.d(TAG, "Google account retrieved: ${account.displayName}, ${account.email}")
                    firebaseAuthWithGoogle(account)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Google Sign-In failed: No account found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                showLoading(false)
                Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
                Toast.makeText(this, "Google Sign-In Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        showLoading(true)

        val idToken = account.idToken
        if (idToken == null) {
            showLoading(false)
            Toast.makeText(this, "Failed to get Google ID token", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    showLoading(false)
                    Toast.makeText(this, "Authentication successful but user is null", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                Log.d(TAG, "Firebase auth successful for: ${currentUser.email}")
                Log.d(TAG, "Display name from Firebase: ${currentUser.displayName}")
                Log.d(TAG, "Display name from Google account: ${account.displayName}")

                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(currentUser.uid)

                userRef.get().addOnSuccessListener { documentSnapshot ->
                    if (!documentSnapshot.exists()) {
                        // Create new user document with Google account data
                        // ✅ Use the Google account data directly, not Firebase Auth
                        val displayName = account.displayName ?: account.givenName ?: account.email?.substringBefore("@") ?: "Student Name"
                        val email = account.email ?: currentUser.email ?: "No Email"
                        val photoUrl = account.photoUrl?.toString() ?: currentUser.photoUrl?.toString()

                        Log.d(TAG, "Creating new user document with name: $displayName")

                        val userData = User(
                            uid = currentUser.uid,
                            fullName = displayName,
                            email = email,
                            profileImageUrl = photoUrl,
                            enrolledDepartmentId = null
                        )

                        userRef.set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User document created successfully")
                                showLoading(false)
                                navigateToHome()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save user: ${e.message}", e)
                                showLoading(false)
                                Toast.makeText(this, "Failed to save user details: ${e.message}", Toast.LENGTH_SHORT).show()
                                navigateToHome() // Still navigate
                            }
                    } else {
                        // User already exists
                        Log.d(TAG, "User document already exists")
                        showLoading(false)
                        navigateToHome()
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to check user document: ${e.message}", e)
                    showLoading(false)
                    Toast.makeText(this, "Failed to check user details: ${e.message}", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            } else {
                showLoading(false)
                Log.e(TAG, "Firebase authentication failed: ${task.exception?.message}", task.exception)
                Toast.makeText(this, "Firebase Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarSignIn.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignIn.isEnabled = !isLoading
        binding.btnSignInGoogle.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}