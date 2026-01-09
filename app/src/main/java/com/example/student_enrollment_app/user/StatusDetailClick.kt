package com.example.student_enrollment_app.user

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.adapter.StatusDetailAdapter
import com.example.student_enrollment_app.databinding.FragmentStatusDetailClickBinding
import com.example.student_enrollment_app.model.StatusDetailItem
import com.example.student_enrollment_app.model.StatusType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatusDetailClick : Fragment(R.layout.fragment_status_detail_click) {

    private var _binding: FragmentStatusDetailClickBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentInvoiceId: String? = null

    companion object {
        private const val TAG = "StatusDetailClick"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusDetailClickBinding.bind(view)

        // Show ActionBar (top bar)
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        // make fragment content NOT fullscreen
        setupEdgeToEdge(false)

        setupClickListeners()
        loadEnrollmentDetails()
    }

    private fun setupClickListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Contact Support button
        binding.btnContactSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Contact support feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // View Invoice button
        binding.btnViewInvoice.setOnClickListener {
            navigateToInvoice()
        }
    }

    private fun loadEnrollmentDetails() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("enrollments")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val status = doc.getString("status") ?: "Pending"
                    val confirmationNumber = doc.getString("confirmationNumber") ?: ""
                    val major = doc.getString("major") ?: "Unknown"
                    val faculty = doc.getString("faculty") ?: "Unknown"
                    currentInvoiceId = doc.getString("invoiceId")

                    displayEnrollmentDetails(status, confirmationNumber, major, faculty)
                } else {
                    Toast.makeText(requireContext(), "No enrollment found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading enrollment: ${e.message}", e)
                Toast.makeText(requireContext(), "Error loading details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayEnrollmentDetails(
        status: String,
        confirmationNumber: String,
        major: String,
        faculty: String
    ) {
        // Calculate progress
        val progress = when (status) {
            "Pending" -> 25
            "Under Review" -> 50
            "Approved" -> 75
            "Confirmed" -> 100
            else -> 0
        }

        binding.overallProgressBar.progress = progress
        binding.tvOverallProgressPercentage.text = "$progress%"

        // Calculate completed steps
        val totalSteps = 4
        val completedSteps = when (status) {
            "Pending" -> 1
            "Under Review" -> 2
            "Approved" -> 3
            "Confirmed" -> 4
            else -> 0
        }
        binding.tvStepsCompleted.text = "$completedSteps/$totalSteps steps completed"

        // Create detail items list
        val detailItems = mutableListOf<StatusDetailItem>()

        // Step 1: Application Submitted
        detailItems.add(
            StatusDetailItem(
                title = "Application Submitted",
                subtitle = "Your enrollment application has been received",
                description = "Faculty: $faculty\nMajor: $major",
                type = StatusType.COMPLETED
            )
        )

        // Step 2: Under Review
        detailItems.add(
            StatusDetailItem(
                title = "Under Review",
                subtitle = if (progress >= 50) "Your application is being reviewed" else "Waiting for review",
                description = if (progress >= 50) "Our admission team is reviewing your documents" else "Application in queue",
                type = when {
                    progress >= 50 -> StatusType.COMPLETED
                    progress >= 25 -> StatusType.IN_PROGRESS
                    else -> StatusType.PENDING
                }
            )
        )

        // Step 3: Approved
        detailItems.add(
            StatusDetailItem(
                title = "Approved",
                subtitle = if (progress >= 75) "Application approved!" else "Pending approval",
                description = if (progress >= 75) "Congratulations! Your application has been approved" else "Waiting for final approval",
                type = when {
                    progress >= 75 -> StatusType.COMPLETED
                    progress >= 50 -> StatusType.IN_PROGRESS
                    else -> StatusType.PENDING
                }
            )
        )

        // Step 4: Confirmation & Invoice
        detailItems.add(
            StatusDetailItem(
                title = "Confirmed & Enrolled",
                subtitle = if (progress == 100) "Enrollment confirmed!" else "Pending confirmation",
                description = if (progress == 100) {
                    "Confirmation Number: $confirmationNumber\nInvoice generated and ready"
                } else {
                    "Final confirmation pending"
                },
                type = when {
                    progress == 100 -> StatusType.COMPLETED
                    progress >= 75 -> StatusType.IN_PROGRESS
                    else -> StatusType.PENDING
                }
            )
        )

        // Setup RecyclerView with detail items
        val adapter = StatusDetailAdapter(detailItems)
        binding.rvStatusDetails.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        // Show/hide invoice button based on invoice availability
        if (!currentInvoiceId.isNullOrEmpty() && progress == 100) {
            binding.btnViewInvoice.visibility = View.VISIBLE
            Log.d(TAG, "Invoice button shown. Invoice ID: $currentInvoiceId")
        } else {
            binding.btnViewInvoice.visibility = View.GONE
            Log.d(TAG, "Invoice button hidden. Invoice ID: $currentInvoiceId, Progress: $progress")
        }
    }

    private fun navigateToInvoice() {
        if (!currentInvoiceId.isNullOrEmpty()) {
            val bundle = Bundle().apply {
                putString("invoiceId", currentInvoiceId)
            }
            try {
                findNavController().navigate(R.id.action_status_detail_to_invoice, bundle)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}", e)
                Toast.makeText(requireContext(), "Unable to open invoice", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Invoice not available yet", Toast.LENGTH_SHORT).show()
        }
    }

    // Control fullscreen or normal layout
    private fun setupEdgeToEdge(showFullscreen: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(
            requireActivity().window,
            !showFullscreen
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}