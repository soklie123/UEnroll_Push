package com.example.student_enrollment_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.databinding.ItemFacultyBinding
import com.example.student_enrollment_app.model.Faculty

class FacultyAdapter(
    private val facultyList: List<Faculty>,
    private val onItemClick: (Faculty) -> Unit
) : RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder>() {

    private var selectedPosition = 0 // Default "All" selected

    inner class FacultyViewHolder(val binding: ItemFacultyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacultyViewHolder {
        val binding = ItemFacultyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FacultyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FacultyViewHolder, position: Int) {
        val faculty = facultyList[position]
        holder.binding.tvFacultyName.text = faculty.name

        // This triggers the XML Selector colors
        holder.binding.tvFacultyName.isSelected = (selectedPosition == position)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onItemClick(faculty)
        }
    }

    override fun getItemCount() = facultyList.size
}