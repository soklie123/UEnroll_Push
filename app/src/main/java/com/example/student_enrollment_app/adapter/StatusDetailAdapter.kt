package com.example.student_enrollment_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.student_enrollment_app.R
import com.example.student_enrollment_app.databinding.ItemDetailStatusBinding
import com.example.student_enrollment_app.model.StatusDetailItem
import com.example.student_enrollment_app.model.StatusType

class StatusDetailAdapter(
    private var items: List<StatusDetailItem>
) : RecyclerView.Adapter<StatusDetailAdapter.DetailViewHolder>() {

    inner class DetailViewHolder(
        private val binding: ItemDetailStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatusDetailItem) {
            // Step number (position + 1)
            binding.tvStepNumber.text = "Step ${adapterPosition + 1}"

            // Title
            binding.tvDetailTitle.text = item.title

            // Subtitle
            binding.tvDetailSubtitle.text = item.subtitle

            // Description (optional)
            if (item.description.isNullOrEmpty()) {
                binding.tvDetailDescription.visibility = View.GONE
            } else {
                binding.tvDetailDescription.visibility = View.VISIBLE
                binding.tvDetailDescription.text = item.description
            }

            // Status indicator and styling based on type
            when (item.type) {
                StatusType.COMPLETED -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.success_green)
                    )
                    binding.cardContainer.strokeColor =
                        binding.root.context.getColor(R.color.success_green)
                    binding.tvStepNumber.setTextColor(
                        binding.root.context.getColor(R.color.success_green)
                    )
                }
                StatusType.IN_PROGRESS -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_in_progress)
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_progress)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.warning_yellow)
                    )
                    binding.cardContainer.strokeColor =
                        binding.root.context.getColor(R.color.warning_yellow)
                    binding.tvStepNumber.setTextColor(
                        binding.root.context.getColor(R.color.warning_yellow)
                    )
                }
                StatusType.PENDING -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_hourglass)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.text_secondary)
                    )
                    binding.cardContainer.strokeColor =
                        binding.root.context.getColor(R.color.border_light)
                    binding.tvStepNumber.setTextColor(
                        binding.root.context.getColor(R.color.text_secondary)
                    )
                }
                StatusType.REJECTED, StatusType.FAILED -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_rejected)
                    binding.imgStatusIcon.setImageResource(R.drawable.ic_close)
                    binding.imgStatusIcon.setColorFilter(
                        binding.root.context.getColor(R.color.status_rejected)
                    )
                    binding.cardContainer.strokeColor =
                        binding.root.context.getColor(R.color.status_rejected)
                    binding.tvStepNumber.setTextColor(
                        binding.root.context.getColor(R.color.status_rejected)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemDetailStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<StatusDetailItem>) {
        items = newList
        notifyDataSetChanged()
    }
}