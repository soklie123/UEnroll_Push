package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentStatusDetailClickBinding

class StatusDetailClick : Fragment(R.layout.fragment_status_detail_click) {

    private var _binding: FragmentStatusDetailClickBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusDetailClickBinding.bind(view)

        val statusId = arguments?.getString("statusId") ?: "No ID"
        binding.tvOverallProgressLabel.text = "Status ID: $statusId"

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
