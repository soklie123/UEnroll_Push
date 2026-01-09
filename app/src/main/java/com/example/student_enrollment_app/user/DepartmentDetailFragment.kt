package com.example.student_enrollment_app.user

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentDepartmentDetailBinding
import com.example.student_enrollment_app.model.Department
import com.google.firebase.firestore.FirebaseFirestore

class DepartmentDetailFragment : Fragment(R.layout.fragment_department_detail) {
    private var _binding: FragmentDepartmentDetailBinding? = null
    private val binding get() = _binding!!
    private var currentDepartment: Department? = null
    private var currentFacultyName: String = "Unknown Faculty"
    private var isNavigating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDepartmentDetailBinding.bind(view)

        binding.btnEnroll.isEnabled = false

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEnroll.setOnClickListener {
            // --- START FIX: Check the flag before navigating ---
            if (isNavigating) return@setOnClickListener

            currentDepartment?.let { dept ->
                // Set the flag to true immediately to block other clicks
                isNavigating = true

                val bundle = Bundle().apply {
                    putString("facultyName", currentFacultyName)
                    putString("departmentName", dept.name ?: "Unknown Department")
                }

                try {
                    findNavController().navigate(R.id.action_detail_to_enrollment, bundle)
                } catch (e: IllegalStateException) {
                    // Log the error just in case, and reset the flag if navigation fails
                    Log.e("DepartmentDetail", "Navigation failed", e)
                    isNavigating = false
                }

            } ?: run {
                Toast.makeText(requireContext(), "Department data not loaded yet!", Toast.LENGTH_SHORT).show()
            }
            // --- END FIX ---
        }

        val departmentId = arguments?.getString("departmentId")
        val facultyId = arguments?.getString("facultyId")

        if (departmentId != null && facultyId != null) {
            loadFacultyName(facultyId)
            loadDepartmentDetails(facultyId, departmentId)
        } else {
            Log.e("DepartmentDetail", "departmentId is null!")
            Toast.makeText(requireContext(), "Invalid department ID", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadFacultyName(facultyId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("faculties")
            .document(facultyId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentFacultyName = document.getString("name") ?: "Unknown Faculty"
                }
            }
            .addOnFailureListener { e ->
                Log.e("DepartmentDetail", "Error loading faculty name", e)
            }
    }

    // --- START FIX: Reset the flag when the user returns to this screen ---
    override fun onResume() {
        super.onResume()
        isNavigating = false
    }
    // --- END FIX ---

    private fun loadDepartmentDetails(facultyId: String?, departmentId: String) {
        val db = FirebaseFirestore.getInstance()
        val facultyFolders = if (facultyId != null) listOf(facultyId) else listOf("engineering", "science", "education")
        var isDataLoaded = false

        for (folder in facultyFolders) {
            if (isDataLoaded) break

            db.collection("faculties")
                .document(folder)
                .collection("departments")
                .document(departmentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        val dept = document.toObject(Department::class.java)
                        dept?.let {
                            currentDepartment = it
                            isDataLoaded = true
                            binding.btnEnroll.isEnabled = true

                            binding.txtDetailTitle.text = it.name ?: "No Name"
                            binding.txtDescription.text = it.description ?: "No Description"
                            binding.txtTuition.text = "$${it.tuition ?: 0} per year"
                            binding.txtDuration.text = it.duration ?: "Unknown"

                            if (!it.logoUrl.isNullOrEmpty()) {
                                Glide.with(requireContext())
                                    .load(it.logoUrl)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .into(binding.imgDetailLogo)
                            } else {
                                binding.imgDetailLogo.setImageResource(R.drawable.ic_placeholder)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error searching in $folder", e)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
