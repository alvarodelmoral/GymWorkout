package com.example.gymworkout.adaptadores

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymworkout.EditTaskActivity
import com.example.gymworkout.R
import com.example.gymworkout.databinding.TaskViewBinding
import com.example.gymworkout.entidades.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class TaskShowAdapter(
    private val taskList: ArrayList<Task>,
    private val context: Context,
    private val showCheckbox: Boolean = false,
    private val onTaskSelected: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskShowAdapter.ViewHolder>() {

    private lateinit var db: FirebaseFirestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.task_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskShowAdapter.ViewHolder, position: Int) {
        val task = taskList[position]
        val stringRepetitions = context.getString(R.string.repetitions_text)
        val stringRepetition = context.getString(R.string.task_repetition)
        val stringSets = context.getString(R.string.sets_Text)
        val stringSet = context.getString(R.string.task_set)
        db = FirebaseFirestore.getInstance()
        holder.setIsRecyclable(false)

        holder.name.text = task.name

        if (task.repetitions == "1") {
            holder.repetitions.text = task.repetitions + " $stringRepetition"
        } else {
            holder.repetitions.text = task.repetitions + " $stringRepetitions"
        }

        if (task.sets == "1") {
            holder.sets.text = task.sets + " $stringSet"
        } else {
            holder.sets.text = task.sets + " $stringSets"
        }

        holder.checkBox.visibility = if (showCheckbox) View.VISIBLE else View.GONE
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = false
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onTaskSelected(task, isChecked)
        }

        holder.itemView.setOnLongClickListener { view ->
            val showMenu = PopupMenu(context, holder.binding.tareaTitulo)
            showMenu.inflate(R.menu.tareas_context_menu)
            showMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.taskEditMenu -> {
                        val intencion = Intent(view.context, EditTaskActivity::class.java)
                        intencion.putExtra("taskId", task.taskId)
                        intencion.putExtra("name", task.name)
                        intencion.putExtra("repetitions", task.repetitions)
                        intencion.putExtra("sets", task.sets)
                        intencion.putExtra("groupId", task.groupId)
                        intencion.putExtra("position", position)
                        context.startActivity(intencion)
                    }

                    R.id.taskDeleteMenu -> {
                        db.collection("tasks").document(task.taskId).delete()
                        taskList.remove(task)
                        notifyItemRemoved(position)
                        notifyDataSetChanged()
                        Snackbar.make(
                            holder.binding.root,
                            context.getString(R.string.deleteTask),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }
            showMenu.show()
            true
        }
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = TaskViewBinding.bind(view)
        val name: TextView = binding.tareaTitulo
        val sets: TextView = binding.tareaSeries
        val repetitions: TextView = binding.tareaRepeticiones
        val checkBox: CheckBox = binding.root.findViewById(R.id.taskCheckBox)
    }
}
