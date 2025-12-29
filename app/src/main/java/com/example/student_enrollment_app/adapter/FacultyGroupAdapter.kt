package com.example.student_enrollment_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.databinding.ItemFacultyGroupBinding
import com.example.student_enrollment_app.model.Department
import com.example.student_enrollment_app.model.FacultyGroup

class FacultyGroupAdapter(
    private var groups: List<FacultyGroup>,
    private val onDepartmentClick: (Department) -> Unit
) : RecyclerView.Adapter<FacultyGroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(val binding: ItemFacultyGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(ItemFacultyGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.tvFacultyName.text = group.facultyName

        // This ensures the red text "X Departments" is always accurate
        val deptCount = group.departments.size
        holder.binding.tvDeptCount.text = "$deptCount ${if (deptCount == 1) "Department" else "Departments"}"

        val childAdapter = DepartmentAdapter(group.departments, onDepartmentClick)
        holder.binding.rvNestedDepartments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = childAdapter
            isNestedScrollingEnabled = false // Fixes the 4-item cutoff issue
        }
    }

    override fun getItemCount() = groups.size

    fun updateData(newGroups: List<FacultyGroup>) {
        this.groups = newGroups
        notifyDataSetChanged()
    }
}