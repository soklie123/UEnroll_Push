package com.example.student_enrollment_app.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.adapter.DepartmentAdapter
import com.example.student_enrollment_app.databinding.FragmentHomeScreenBinding
import com.example.student_enrollment_app.model.Department
import com.example.student_enrollment_app.repository.DepartmentRepository
import com.example.student_enrollment_app.repository.FacultyRepository
import com.example.student_enrollment_app.repository.UserRepository
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class HomeScreenFragment : Fragment() {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    // Initialize repositories
    private val userRepository = UserRepository()
    private val facultyRepository = FacultyRepository()
    private val departmentRepository = DepartmentRepository()

    private lateinit var departmentAdapter: DepartmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadUserData()
        loadFaculties()
        loadAllDepartments()

        binding.searchCard.setOnClickListener {
            // Implement search logic
        }
    }

    private fun setupRecyclerView() {
        departmentAdapter = DepartmentAdapter(emptyList()) { department ->
            // Handle department click
        }

        binding.recyclerDepartments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = departmentAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val currentUser = userRepository.getCurrentUser()

            val userName = if (currentUser != null) {
                // Priority: custom username > name > fullName > email username
                currentUser.username ?:
                currentUser.name ?:
                currentUser.fullName ?:
                userRepository.extractUsernameFromEmail(currentUser.email)
            } else {
                "Student"
            }

            binding.tvUserName.text = "hi $userName"
        }
    }

    private fun loadFaculties() {
        lifecycleScope.launch {
            val faculties = facultyRepository.getAllFaculties()

            binding.chipGroupFaculties.removeAllViews()

            // Add "All" chip
            val allChip = createChip("All", true) {
                loadAllDepartments()
                binding.tvSelectedFaculty.visibility = View.GONE
            }
            binding.chipGroupFaculties.addView(allChip)

            // Add faculty chips
            faculties.forEach { faculty ->
                val chip = createChip(faculty.name, false) {
                    loadDepartments(faculty.id)
                    binding.tvSelectedFaculty.apply {
                        text = faculty.name
                        visibility = View.VISIBLE
                    }
                }
                binding.chipGroupFaculties.addView(chip)
            }
        }
    }

    private fun createChip(label: String, isSelected: Boolean, onClick: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = isSelected

            try {
                setChipBackgroundColorResource(R.color.chip_background)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text))
                chipStrokeWidth = 1f
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke)
            } catch (e: Exception) {
                // Fallback
            }

            setOnClickListener {
                val group = parent as? ChipGroup
                group?.let {
                    for (i in 0 until it.childCount) {
                        val child = it.getChildAt(i) as? Chip
                        child?.isChecked = (child == this)
                    }
                }
                onClick()
            }
        }
    }

    private fun loadAllDepartments() {
        lifecycleScope.launch {
            binding.recyclerDepartments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            val departments = departmentRepository.getAllDepartments()
            updateUI(departments)
        }
    }

    private fun loadDepartments(facultyId: String) {
        lifecycleScope.launch {
            binding.recyclerDepartments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            val departments = departmentRepository.getDepartmentsByFaculty(facultyId)
            updateUI(departments)
        }
    }

    private fun updateUI(list: List<Department>) {
        if (list.isEmpty()) {
            showEmptyState()
        } else {
            binding.recyclerDepartments.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            // DEBUG: Check department data
            list.forEach { department ->
                println("""
                DEBUG: Department: ${department.name}
                Seats: ${department.seats}
                Seats Available: ${department.seatsAvailable}
                Percentage: ${if (department.seats > 0)
                    (department.seatsAvailable.toDouble() / department.seats.toDouble() * 100).toInt()
                else 0}%
            """.trimIndent())
            }

            departmentAdapter.updateList(list)
        }
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