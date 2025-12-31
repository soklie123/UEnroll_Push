package com.example.student_enrollment_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.FragmentDepartmentDetailBinding
import com.example.student_enrollment_app.model.Department
import com.google.firebase.firestore.FirebaseFirestore

class DepartmentDetailFragment : Fragment(R.layout.fragment_department_detail) {

    private var _binding: FragmentDepartmentDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDepartmentDetailBinding.bind(view)

        // Back button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Enroll button
        binding.btnEnroll.setOnClickListener {
            val departmentName = binding.txtDetailTitle.text.toString()
            val facultyName = arguments?.getString("facultyName") ?: "Engineering"

            val intent = Intent(requireContext(), EnrollActivity::class.java).apply {
                putExtra("FACULTY_NAME", facultyName)
                putExtra("MAJOR_NAME", departmentName)
            }
            startActivity(intent)
        }

        // Get arguments
        val departmentId = arguments?.getString("departmentId")
        val facultyId = arguments?.getString("facultyId")

        if (departmentId != null) {
            loadDepartmentDetails(facultyId, departmentId)
        } else {
            Log.e("DepartmentDetail", "departmentId is null!")
        }
    }

    private fun loadDepartmentDetails(facultyId: String?, departmentId: String) {
        val db = FirebaseFirestore.getInstance()
        var isDataLoaded = false

        val facultyFolders = if (facultyId != null) {
            listOf(facultyId)
        } else {
            listOf("engineering", "science", "education")
        }

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
                            isDataLoaded = true

                            // Set department info
                            binding.txtDetailTitle.text = it.name
                            binding.txtDescription.text = it.description
                            binding.txtTuition.text = "$${it.tuition} per year"
                            binding.txtDuration.text = it.duration

                            // Load department logo
                            if (!it.logoUrl.isNullOrEmpty()) {
                                Glide.with(requireContext())
                                    .load(it.logoUrl)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .into(binding.imgDetailLogo)
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
