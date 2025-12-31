package com.example.student_enrollment_app.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.util.Calendar

class HomeScreenFragment : Fragment() {
    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()
    private var isNavigating = false
    private val facultyRepository = FacultyRepository()
    private val departmentRepository = DepartmentRepository()
    private lateinit var facultyGroupAdapter: FacultyGroupAdapter
    private var currentFacultyGroups: List<FacultyGroup> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        loadUserData()
        loadFaculties()
        loadAllGroupedData()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun filterData(query: String){
        val filteredList = if(query.isBlank()){
            currentFacultyGroups
        }else{
            val lowerCaseQuery = query.lowercase()
            currentFacultyGroups.mapNotNull { facultyGroup ->
                val matchingDepartments = facultyGroup.departments.filter { department ->
                    department.name.lowercase().contains(lowerCaseQuery)
                }
                if(matchingDepartments.isNotEmpty()){
                    facultyGroup.copy(departments = matchingDepartments)
                }else{
                    null
                }
            }
        }
        facultyGroupAdapter.updateData(filteredList)
    }

    private fun navigateToDetail(department: Department) {
        if(isNavigating) return
        isNavigating = true
        try {

            if (findNavController().currentDestination?.id == R.id.homeScreenFragment) {
                val bundle = Bundle().apply {
                    putString("departmentId", department.id)
                }

                findNavController().navigate(R.id.action_home_to_detail, bundle)
            }
        } catch (e: Exception) {
            android.util.Log.e("NavError", "Navigation failed: ${e.message}")
        }
        binding.root.postDelayed({
            isNavigating = false
        }, 500)
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
        viewLifecycleOwner.lifecycleScope.launch {
            val user = userRepository.getCurrentUser()
            val name = user?.username ?: user?.name ?: "Student"
            // Dynamic greeting for better UX
            val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good Morning!"
                in 12..16 -> "Good Afternoon!"
                else -> "Good Evening!"
            }
            binding.tvGreeting.text = greeting
            binding.tvUserName.text = "Hi $name!"
        }
    }

    private fun loadFaculties() {
        lifecycleScope.launch {
            val faculties = facultyRepository.getAllFaculties()
            binding.chipGroupFaculties.removeAllViews()

            binding.chipGroupFaculties.addView(createChip("All", true) {
                binding.etSearch.text.clear()
                loadAllGroupedData()
            })

            faculties.forEach { faculty ->
                binding.chipGroupFaculties.addView(createChip(faculty.name, false) {
                    binding.etSearch.text.clear()
                    loadSingleFacultyGroup(faculty.id, faculty.name)
                })
            }
        }
    }

    private fun loadAllGroupedData() {
        lifecycleScope.launch {
            try {
                val groupedData = departmentRepository.getGroupedDepartments(facultyRepository)
                currentFacultyGroups = groupedData
                if (groupedData.isEmpty()) {
                    showEmptyState()
                } else {
                    showDataState()
                    facultyGroupAdapter.updateData(groupedData)
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
            currentFacultyGroups = singleGroup  // Cache the data
            updateGroupedUI(singleGroup)
        }
    }

    private fun updateGroupedUI(groups: List<FacultyGroup>) {
        toggleLoading(false)
        if (groups.isEmpty() || (groups.size == 1 && groups[0].departments.isEmpty())) {
            binding.recyclerDepartments.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            showDataState()
            facultyGroupAdapter.updateData(groups)
        }
    }

    private fun toggleLoading(isLoading: Boolean) {

    }
    private fun createChip(label: String, isSelected: Boolean, onSelected: () -> Unit): Chip {
        val inflater = LayoutInflater.from(binding.chipGroupFaculties.context)
        val chip = inflater.inflate(R.layout.chip_filter_layout, binding.chipGroupFaculties, false) as Chip
        return chip.apply {
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