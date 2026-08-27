package com.example.emr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val onCompleteClick: (TaskItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val items = mutableListOf<TaskItem>()

    fun submitList(newItems: List<TaskItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val patientText: TextView = itemView.findViewById(R.id.patientText)
        private val taskNameText: TextView = itemView.findViewById(R.id.taskNameText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val detailText: TextView = itemView.findViewById(R.id.detailText)
        private val statusBadgeText: TextView = itemView.findViewById(R.id.statusBadgeText)
        private val completeButton: Button = itemView.findViewById(R.id.completeButton)

        fun bind(item: TaskItem) {
            patientText.text =
                "${item.patient_code.ifBlank { "-" }} / ${item.room_number.ifBlank { "-" }}호 / ${item.patient_name.ifBlank { "-" }}"

            taskNameText.text = item.task_name.ifBlank { "업무명 없음" }

            descriptionText.text =
                item.description.ifBlank { "업무 설명 없음" }

            detailText.text =
                "업무 순서: ${item.priority.ifBlank { "-" }} / 담당자: ${item.assigned_to.ifBlank { "-" }} / 코드: ${item.assigned_to_code.ifBlank { "-" }}"

            val isCompleted = item.status == "completed"

            statusBadgeText.text = if (isCompleted) "완료" else "미완료"
            statusBadgeText.setBackgroundColor(
                if (isCompleted) 0xFF10B981.toInt() else 0xFFF59E0B.toInt()
            )

            completeButton.isEnabled = !isCompleted
            completeButton.text = if (isCompleted) "완료됨" else "처치완료"

            completeButton.setOnClickListener {
                if (!isCompleted) {
                    onCompleteClick(item)
                }
            }
        }
    }
}