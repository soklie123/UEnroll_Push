package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.adapter.StatusItemAdapter
import com.example.student_enrollment_app.databinding.FragmentStatusScreenBinding
import com.example.student_enrollment_app.model.StatusItem
import com.example.student_enrollment_app.model.StatusType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatusScreenFragment : Fragment(R.layout.fragment_status_screen) {

    private var _binding: FragmentStatusScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: StatusItemAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentInvoiceId: String? = null
    private var hasEnrollment = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusScreenBinding.bind(view)

        setupRecyclerView()
        setupClickListeners()
        fetchEnrollmentStatus()
    }

    private fun setupClickListeners() {
        // View Detail Button
        binding.btnViewDetail.setOnClickListener {
            if (hasEnrollment) {
                findNavController().navigate(R.id.action_status_to_detail)
            } else {
                Toast.makeText(requireContext(), "No enrollment found", Toast.LENGTH_SHORT).show()
            }
        }

        // View Invoice Button
        binding.btnViewInvoice.setOnClickListener {
            navigateToInvoice()
        }
    }

    private fun setupRecyclerView() {
        adapter = StatusItemAdapter(emptyList())
        binding.rvStatusItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@StatusScreenFragment.adapter
        }
    }

    private fun fetchEnrollmentStatus() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Listen for enrollment documents for the current user in real-time
        db.collection("enrollments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    hasEnrollment = true
                    val doc = snapshot.documents[0]  // Assume only one enrollment per user
                    val status = doc.getString("status") ?: "Pending"
                    val confirmationNumber = doc.getString("confirmationNumber") ?: ""
                    val major = doc.getString("major") ?: "Unknown"
                    val faculty = doc.getString("faculty") ?: "Unknown"
                    currentInvoiceId = doc.getString("invoiceId")

                    // Show/hide invoice button based on invoice availability
                    if (!currentInvoiceId.isNullOrEmpty()) {
                        binding.btnViewInvoice.visibility = View.VISIBLE
                    } else {
                        binding.btnViewInvoice.visibility = View.GONE
                    }

                    // Calculate progress
                    val progress = when (status) {
                        "Pending" -> 33
                        "Approved" -> 66
                        "Confirmed" -> 100
                        else -> 0
                    }
                    binding.progressBar.progress = progress
                    binding.tvProgressPercentage.text = "$progress%"

                    // Build dynamic list for RecyclerView
                    val statusList = listOf(
                        StatusItem(
                            title = "Faculty / Major",
                            subtitle = "$faculty / $major",
                            type = StatusType.COMPLETED
                        ),
                        StatusItem(
                            title = "Enrollment Status",
                            subtitle = if (status == "Confirmed") "Confirmed! Number: $confirmationNumber" else status,
                            type = when(status) {
                                "Confirmed" -> StatusType.COMPLETED
                                "Approved" -> StatusType.IN_PROGRESS
                                "Rejected" -> StatusType.REJECTED
                                else -> StatusType.PENDING
                            }
                        ),
                        StatusItem(
                            title = "Documents Uploaded",
                            subtitle = "ID & Photo uploaded",
                            type = StatusType.COMPLETED
                        ),
                        StatusItem(
                            title = "Invoice Generated",
                            subtitle = if (!currentInvoiceId.isNullOrEmpty()) "Invoice available" else "Pending",
                            type = if (!currentInvoiceId.isNullOrEmpty()) StatusType.COMPLETED else StatusType.PENDING
                        )
                    )

                    adapter.updateList(statusList)

                    // Optional: pop-up notification when enrollment confirmed
                    if (status == "Confirmed") {
                        Toast.makeText(requireContext(), "🎉 Enrollment Confirmed! Number: $confirmationNumber", Toast.LENGTH_LONG).show()
                    }

                } else {
                    hasEnrollment = false
                    binding.btnViewInvoice.visibility = View.GONE
                    binding.progressBar.progress = 0
                    binding.tvProgressPercentage.text = "0%"
                    Toast.makeText(requireContext(), "No enrollment found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToInvoice() {
        if (!currentInvoiceId.isNullOrEmpty()) {
            // Navigate with specific invoice ID
            val bundle = Bundle().apply {
                putString("invoiceId", currentInvoiceId)
            }
            findNavController().navigate(R.id.action_status_to_invoice, bundle)
        } else {
            // Navigate to load latest invoice
            findNavController().navigate(R.id.action_status_to_invoice)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}