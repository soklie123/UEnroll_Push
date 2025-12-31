package com.example.student_enrollment_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.ItemStatusBinding
import com.example.student_enrollment_app.model.StatusItem
import com.example.student_enrollment_app.model.StatusType

class StatusItemAdapter(
    private val items: List<StatusItem>
) : RecyclerView.Adapter<StatusItemAdapter.StatusViewHolder>() {

    inner class StatusViewHolder(
        private val binding: ItemStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatusItem) {

            binding.tvStatusTitle.text = item.title

            // Subtitle (optional)
            if (item.subtitle.isNullOrEmpty()) {
                binding.tvStatusSubtitle.visibility = View.GONE
            } else {
                binding.tvStatusSubtitle.visibility = View.VISIBLE
                binding.tvStatusSubtitle.text = item.subtitle
            }

            // Icon + color based on status
            when (item.type) {
                StatusType.COMPLETED -> {
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.green)
                    )
                }

                StatusType.PENDING -> {
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_hourglass)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.warning_yellow)
                    )
                }

                StatusType.FAILED -> {
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_close)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.orange)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val binding = ItemStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
