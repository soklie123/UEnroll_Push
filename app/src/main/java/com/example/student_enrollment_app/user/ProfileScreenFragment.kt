package com.example.student_enrollment_app.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.FragmentProfileScreenBinding
import com.example.student_enrollment_app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileScreenFragment : Fragment() {

    private var _binding: FragmentProfileScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "ProfileScreenFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Hide edit photo button (can be enabled later if needed)
        binding.fabEditPhoto.visibility = View.GONE

        setupLogoutButton()
    }

    override fun onResume() {
        super.onResume()
        // Load profile data every time fragment becomes visible
        loadUserProfile()
    }

    private fun setupLogoutButton() {
        binding.btnLogoutAction.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToSignIn()
        }
        // Setup View Invoice Button
        binding.btnViewInvoice.setOnClickListener {
            navigateToInvoice()
        }
    }

    private fun navigateToInvoice() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val invoiceId = document.getString("invoiceId")

                if (invoiceId.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "No invoice available", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val bundle = Bundle().apply {
                    putString("invoiceId", invoiceId)
                }

                findNavController().navigate(
                    R.id.action_profile_to_invoice,
                    bundle
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load invoice", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadUserProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.w(TAG, "No authenticated user found")
            navigateToSignIn()
            return
        }

        Log.d(TAG, "Loading profile for UID: ${currentUser.uid}")

        // Fetch user document from Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        // Manually construct User object from document
                        val user = User(
                            uid = document.getString("uid") ?: currentUser.uid,
                            fullName = document.getString("fullName") ?: "Student Name",
                            email = document.getString("email") ?: currentUser.email ?: "No Email",
                            enrolledDepartmentId = document.getString("enrolledDepartmentId"),
                            profileImageUrl = document.getString("profileImageUrl")
                        )

                        Log.d(TAG, "Successfully loaded user: ${user.fullName}")
                        displayUserInfo(user)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data: ${e.message}", e)
                        Toast.makeText(requireContext(), "Error loading profile data", Toast.LENGTH_SHORT).show()
                        displayFallbackInfo()
                    }
                } else {
                    // Document doesn't exist - this shouldn't happen after proper sign-up/sign-in
                    Log.w(TAG, "User document not found in Firestore for UID: ${currentUser.uid}")
                    Toast.makeText(requireContext(), "Creating profile...", Toast.LENGTH_SHORT).show()
                    createMissingUserDocument(currentUser.uid)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to load user document: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                displayFallbackInfo()
            }
    }

    private fun createMissingUserDocument(uid: String) {
        val currentUser = auth.currentUser ?: return

        val userData = User(
            uid = uid,
            fullName = currentUser.displayName ?: "Student Name",
            email = currentUser.email ?: "No Email",
            enrolledDepartmentId = null,
            profileImageUrl = currentUser.photoUrl?.toString()
        )

        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User document created successfully")
                displayUserInfo(userData)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create user document: ${e.message}", e)
                displayFallbackInfo()
            }
    }

    private fun displayUserInfo(user: User) {
        // Display full name - with fallback
        val displayName = if (user.fullName.isNotEmpty() && user.fullName != "Student Name") {
            user.fullName
        } else {
            // If fullName is empty or default, try to get from Firebase Auth
            auth.currentUser?.displayName ?: user.fullName
        }
        binding.userName.text = displayName

        // Display email
        binding.tvDisplayEmail.text = user.email

        // Generate and display user ID from UID
        val uniqueCode = user.uid.take(6).uppercase()
        binding.userId.text = "ID: STU-2026-$uniqueCode"

        // Display department or "Not Enrolled"
        binding.tvDisplayDepartment.text = if (user.enrolledDepartmentId.isNullOrEmpty()) {
            "Not Enrolled"
        } else {
            user.enrolledDepartmentId
        }

        // Load profile image with Glide
        Glide.with(this)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(binding.profileImage)

        Log.d(TAG, "Profile displayed - Name: $displayName, Email: ${user.email}")
    }

    private fun displayFallbackInfo() {
        val currentUser = auth.currentUser ?: return

        // Use Firebase Auth data as fallback
        binding.userName.text = currentUser.displayName ?: "Student Name"
        binding.tvDisplayEmail.text = currentUser.email ?: "user@gmail.com"

        val uniqueCode = currentUser.uid.take(6).uppercase()
        binding.userId.text = "ID: STU-2026-$uniqueCode"
        binding.tvDisplayDepartment.text = "Not Enrolled"

        // Load profile photo from Firebase Auth
        Glide.with(this)
            .load(currentUser.photoUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(binding.profileImage)

        Log.d(TAG, "Displaying fallback info for: ${currentUser.displayName}")
    }

    private fun navigateToSignIn() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}