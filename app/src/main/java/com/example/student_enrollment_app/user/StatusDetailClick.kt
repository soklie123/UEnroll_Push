package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentStatusDetailClickBinding

class StatusDetailClick : Fragment(R.layout.fragment_status_detail_click) {

    private var _binding: FragmentStatusDetailClickBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusDetailClickBinding.bind(view)

        // Show ActionBar (top bar)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        // make fragment content NOT fullscreen
        setupEdgeToEdge(false)

        // Back button
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Set status ID
        val statusId = arguments?.getString("statusId") ?: "No ID"
        binding.tvOverallProgressLabel.text = "Status ID: $statusId"
    }

    // Control fullscreen or normal layout
    private fun setupEdgeToEdge(showFullscreen: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(
            requireActivity().window,  // Activity window
            !showFullscreen            // false = normal layout with top bar
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
