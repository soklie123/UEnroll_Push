package com.example.student_enrollment_app.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Ensure you have Glide dependency
import com.example.student_enrollment_app.R
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

            if (!department.logoUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(department.logoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery) // Default Android icon
                    .error(android.R.drawable.stat_notify_error)   // Default Android error icon
                    .into(binding.imgDeptLogo)           // This ID must be in your XML
            } else {
                // Set a default icon if no URL exists in Firebase
                binding.imgDeptLogo.setImageResource(R.drawable.ic_default_dept)
            }

            // --- TEXT AND COLORS ---
            binding.txtSeats.setTextColor(Color.parseColor("#E0E0E0"))
            binding.txtDeptName.text = department.name
            binding.txtDeptName.setTextColor(Color.WHITE)
            binding.txtSeats.text = "${department.seatsAvailable}/${department.seats} seats available"

            val backgroundColor = try {
                Color.parseColor(department.color)
            } catch (e: Exception) {
                Color.parseColor("#2196F3")
            }
            binding.cardDepartment.setCardBackgroundColor(backgroundColor)

            // --- PROGRESS BAR LOGIC ---
            val progress = if (department.seats > 0) {
                ((department.seatsAvailable.toDouble() / department.seats.toDouble()) * 100).toInt()
            } else {
                0
            }
            binding.progressSeats.progress = progress
            binding.progressSeats.progressTintList = ColorStateList.valueOf(Color.WHITE)
            binding.progressSeats.progressBackgroundTintList = ColorStateList.valueOf(Color.WHITE)

            binding.root.setOnClickListener { view ->
                if(!view.isClickable) return@setOnClickListener
                view.isEnabled = false
                onItemClick(department)

                view.postDelayed({
                    view.isEnabled = true
                }, 500)
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
        this.departments = newList
        notifyDataSetChanged()
    }
}