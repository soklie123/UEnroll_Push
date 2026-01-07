package com.example.student_enrollment_app.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.student_enrollment_app.auth.SignInActivity
import com.example.student_enrollment_app.databinding.FragmentProfileScreenBinding
import com.example.student_enrollment_app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class ProfileScreenFragment : Fragment() {

    private var _binding: FragmentProfileScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        // Always load fresh data
        loadUserProfile()

        // Logout
        binding.btnLogoutAction.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject<User>()
                user?.let {
                    bindUser(it)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun bindUser(user: User) {
        binding.userName.text = user.fullName
        binding.tvDisplayEmail.text = user.email

        val uniqueCode = user.uid.take(6).uppercase()
        binding.userId.text = "ID: STU-2026-$uniqueCode"

        binding.tvDisplayDepartment.text =
            user.enrolledDepartmentId ?: "Not Enrolled"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}