package com.example.gymworkout.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymworkout.R
import com.example.gymworkout.entidades.Day

class DayShowAdapter(
    private val days: List<Day>,
    private val onDayClicked: (Day) -> Unit,
    private val onAddTaskClicked: (Day) -> Unit
) : RecyclerView.Adapter<DayShowAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_view, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.dayNameTextView.text = day.name
        holder.taskListTextView.text = day.tasks.joinToString(separator = "\n") { it.name }
        holder.itemView.setOnClickListener { onDayClicked(day) }
        holder.addTaskButton.setOnClickListener { onAddTaskClicked(day) }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayNameTextView: TextView = itemView.findViewById(R.id.dayNameTextView)
        val taskListTextView: TextView = itemView.findViewById(R.id.taskListTextView)
        val addTaskButton: Button = itemView.findViewById(R.id.addTaskButton)
    }
}