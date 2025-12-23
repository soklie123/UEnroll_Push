package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.student_enrollment_app.databinding.FragmentHomeScreenBinding
import com.google.firebase.auth.FirebaseAuth

class HomeScreenFragment : Fragment() {
    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    // CORRECTED: The variable should be of type FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return type should be non-nullable View
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the auth variable
        auth = FirebaseAuth.getInstance()

        updateWelcomeText()
        setupClickListeners()
    }

    private fun updateWelcomeText() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userName =
                if (currentUser.displayName.isNullOrEmpty()) {
                    currentUser.email
                } else {
                    currentUser.displayName
                }
            binding.welcomeText.text = "Welcome ${userName ?: "Student"}! 👋"
        } else {
            binding.welcomeText.text = "Welcome Student! 👋"
        }
    } // CORRECTED: Added the missing closing brace here

    private fun setupClickListeners() {
        // Example for the "See more" TextView
        binding.facultySeeMore.setOnClickListener {
            // TODO: Navigate to a new screen showing all faculties
        }

        // Example for handling search
        binding.searchInput.setOnEditorActionListener { v, actionId, event ->
            // TODO: Handle the search action
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding object to avoid memory leaks
        _binding = null
    }
}
