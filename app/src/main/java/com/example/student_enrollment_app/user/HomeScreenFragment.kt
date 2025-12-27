package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.adapter.FacultyGroupAdapter
import com.example.student_enrollment_app.databinding.FragmentHomeScreenBinding
import com.example.student_enrollment_app.model.Department
import com.example.student_enrollment_app.model.FacultyGroup
import com.example.student_enrollment_app.repository.DepartmentRepository
import com.example.student_enrollment_app.repository.FacultyRepository
import com.example.student_enrollment_app.repository.UserRepository
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class HomeScreenFragment : Fragment() {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val facultyRepository = FacultyRepository()
    private val departmentRepository = DepartmentRepository()

    private lateinit var facultyGroupAdapter: FacultyGroupAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadUserData()
        loadFaculties()
        loadAllGroupedData()
    }

    private fun navigateToDetail(department: Department) {
        try {
            // Safety check to prevent crashing if user clicks twice fast
            if (findNavController().currentDestination?.id == R.id.homeScreenFragment) {
                val bundle = Bundle().apply {
                    putString("departmentId", department.id)
                }
                // This ID must match the <action> android:id in your nav_graph.xml
                findNavController().navigate(R.id.action_home_to_detail, bundle)
            }
        } catch (e: Exception) {
            android.util.Log.e("NavError", "Navigation failed: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        facultyGroupAdapter = FacultyGroupAdapter(emptyList()) { department ->
            navigateToDetail(department) // Added navigation
        }
        binding.recyclerDepartments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = facultyGroupAdapter
        }
    }


    private fun loadUserData() {
        lifecycleScope.launch {
            val user = userRepository.getCurrentUser()
            val name = user?.username ?: user?.name ?: "Student"
            binding.tvUserName.text = "hi $name"
        }
    }

    private fun loadFaculties() {
        lifecycleScope.launch {
            val faculties = facultyRepository.getAllFaculties()
            binding.chipGroupFaculties.removeAllViews()

            binding.chipGroupFaculties.addView(createChip("All", true) {
                loadAllGroupedData()
            })

            faculties.forEach { faculty ->
                binding.chipGroupFaculties.addView(createChip(faculty.name, false) {
                    loadSingleFacultyGroup(faculty.id, faculty.name)
                })
            }
        }
    }

    private fun loadAllGroupedData() {
        lifecycleScope.launch {
            try {
                val groupedData = departmentRepository.getGroupedDepartments(facultyRepository)
                if (groupedData.isEmpty()) {
                    showEmptyState()
                } else {
                    showDataState()
                    // Updated with navigation click listener
                    facultyGroupAdapter = FacultyGroupAdapter(groupedData) { department ->
                        navigateToDetail(department)
                    }
                    binding.recyclerDepartments.adapter = facultyGroupAdapter
                }
            } catch (e: Exception) {
                showEmptyState()
            }
        }
    }

    private fun loadSingleFacultyGroup(facultyId: String, facultyName: String) {
        lifecycleScope.launch {
            toggleLoading(true)
            val departments = departmentRepository.getDepartmentsByFaculty(facultyId)
            val singleGroup = listOf(FacultyGroup(facultyId, facultyName, departments))
            updateGroupedUI(singleGroup)
        }
    }

    private fun updateGroupedUI(groups: List<FacultyGroup>) {
        toggleLoading(false)
        if (groups.isEmpty() || (groups.size == 1 && groups[0].departments.isEmpty())) {
            binding.recyclerDepartments.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerDepartments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            // Updated with navigation click listener
            facultyGroupAdapter = FacultyGroupAdapter(groups) { department ->
                navigateToDetail(department)
            }
            binding.recyclerDepartments.adapter = facultyGroupAdapter
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        // Implementation for progress bar if needed
    }

    private fun createChip(label: String, isSelected: Boolean, onSelected: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = isSelected
            setOnClickListener { onSelected() }
        }
    }

    private fun showDataState() {
        binding.recyclerDepartments.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.recyclerDepartments.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}