package com.example.student_enrollment_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.databinding.ItemDepartmentBinding
import com.example.student_enrollment_app.model.Department

class DepartmentAdapter(
    private var departments: List<Department>,
    private val onItemClick: (Department) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.DepartmentViewHolder>() {

    class DepartmentViewHolder(
        private val binding: ItemDepartmentBinding,
        private val onItemClick: (Department) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(department: Department) {
            binding.txtDeptName.text = department.name
            binding.txtSeats.text = "${department.seatsAvailable}/${department.seats} seats available"

            // Set the background color from Firestore
            val backgroundColor = try {
                // Parse the color string (could be hex or color name)
                android.graphics.Color.parseColor(department.color)
            } catch (e: Exception) {
                // Fallback color if parsing fails
                android.graphics.Color.parseColor("#2196F3")
            }

            binding.cardDepartment.setCardBackgroundColor(backgroundColor)

            // Calculate and set progress
            val progress = if (department.seats > 0) {
                ((department.seatsAvailable.toDouble() / department.seats.toDouble()) * 100).toInt()
            } else {
                0
            }

            binding.progressSeats.progress = progress

            // Set click listener
            binding.root.setOnClickListener {
                onItemClick(department)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = ItemDepartmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DepartmentViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.bind(departments[position])
    }

    override fun getItemCount(): Int = departments.size

    fun updateList(newList: List<Department>) {
        departments = newList
        notifyDataSetChanged()
    }
}