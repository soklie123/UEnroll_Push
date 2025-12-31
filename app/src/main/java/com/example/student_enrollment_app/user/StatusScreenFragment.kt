package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.adapter.StatusItemAdapter
import com.example.student_enrollment_app.databinding.FragmentStatusScreenBinding
import com.example.student_enrollment_app.model.StatusItem
import com.example.student_enrollment_app.model.StatusType

class StatusScreenFragment : Fragment(R.layout.fragment_status_screen) {

    private var _binding: FragmentStatusScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: StatusItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusScreenBinding.bind(view)

        setupRecyclerView()
        setupViewDetailButton()
    }

    private fun setupRecyclerView() {
        val statusList = listOf(
            StatusItem(
                title = "Payment Pending",
                subtitle = "Due: Nov 30",
                type = StatusType.PENDING
            ),
            StatusItem(
                title = "Enrollment Not Confirmed",
                subtitle = null,
                type = StatusType.FAILED
            ),
            StatusItem(
                title = "Documents Submitted",
                subtitle = "2 of 3 uploaded",
                type = StatusType.PENDING
            ),
            StatusItem(
                title = "Major Selected",
                subtitle = "IT Engineering",
                type = StatusType.COMPLETED
            )
        )

        binding.rvStatusItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = StatusItemAdapter(statusList)
        }
    }

    private fun setupViewDetailButton() {
        binding.btnViewDetail.setOnClickListener {
            val bundle = Bundle().apply { putString("statusId", "status_123") }
            findNavController().navigate(R.id.action_status_to_detail, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
