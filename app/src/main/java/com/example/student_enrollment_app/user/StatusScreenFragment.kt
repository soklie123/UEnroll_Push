package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusScreenBinding.bind(view)

        setupRecyclerView()
        fetchEnrollmentStatus()
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
                    val doc = snapshot.documents[0]  // Assume only one enrollment per user
                    val status = doc.getString("status") ?: "Pending"
                    val confirmationNumber = doc.getString("confirmationNumber") ?: ""
                    val major = doc.getString("major") ?: "Unknown"
                    val faculty = doc.getString("faculty") ?: "Unknown"

                    // Build dynamic list for RecyclerView
                    val statusList = listOf(
                        StatusItem(
                            title = "Faculty / Major",
                            subtitle = "$faculty / $major",
                            type = StatusType.COMPLETED
                        ),
                        StatusItem(
                            title = "Enrollment Status",
                            subtitle = if (status == "Confirmed") "Confirmed! Number: $confirmationNumber" else "Pending",
                            type = if (status == "Confirmed") StatusType.COMPLETED else StatusType.PENDING
                        ),
                        StatusItem(
                            title = "Documents Uploaded",
                            subtitle = "ID & Photo uploaded",
                            type = StatusType.COMPLETED
                        )
                    )

                    adapter.updateList(statusList)

                    // Optional: pop-up notification when enrollment confirmed
                    if (status == "Confirmed") {
                        Toast.makeText(requireContext(), "🎉 Enrollment Confirmed! Number: $confirmationNumber", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(requireContext(), "No enrollment found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
