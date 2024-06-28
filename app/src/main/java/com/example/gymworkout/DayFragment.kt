package com.example.gymworkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymworkout.adaptadores.DayShowAdapter
import com.example.gymworkout.adaptadores.TaskShowAdapter
import com.example.gymworkout.databinding.FragmentDayBinding
import com.example.gymworkout.entidades.Day
import com.example.gymworkout.entidades.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import android.app.AlertDialog
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DayFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var dayArrayList: ArrayList<Day>
    private lateinit var dayAdapter: DayShowAdapter
    private lateinit var auth: FirebaseAuth

    private var _binding: FragmentDayBinding? = null
    private val binding get() = _binding!!

    private val daysOfWeek = listOf(
        Day("Lunes"),
        Day("Martes"),
        Day("Miercoles"),
        Day("Jueves"),
        Day("Viernes"),
        Day("Sabado"),
        Day("Domingo")
    )

    private val availableTasks = ArrayList<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDayBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        dayArrayList = arrayListOf()

        dayAdapter = DayShowAdapter(daysOfWeek, { day ->
            // Handle day click
            Toast.makeText(context, "Esto es ${day.name}", Toast.LENGTH_SHORT).show()
            fetchTasksForDay(day)
        }, { day ->
            // Handle add task click
            fetchAvailableTasks { showSelectTaskDialog(day) }
        })

        binding.recyclerViewDays.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = dayAdapter
        }

        // Load tasks for all days when fragment is created
        daysOfWeek.forEach { fetchTasksForDay(it) }

        binding.menuButtonDays.setOnClickListener {
            showPopup(binding.menuButtonDays)
        }
    }

    private fun calldb() {
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val groupID = prefs?.getString("group", "")

        db = FirebaseFirestore.getInstance()
        db.collection("tasks").whereEqualTo("group", groupID).addSnapshotListener(object :
            EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                if (error != null) {
                    Log.i("Carencias en firestore", error.message.toString())
                    return
                }
                for (dc: DocumentChange in value?.documentChanges!!) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        dayArrayList.add(dc.document.toObject(Day::class.java))
                    }
                }
                dayAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun fetchTasksForDay(day: Day) {
        val db = FirebaseFirestore.getInstance()
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val groupID = prefs?.getString("group", "")

        db.collection("days").document(day.name).collection("tasks")
            .whereEqualTo("groupId", groupID)
            .get()
            .addOnSuccessListener { result ->
                day.tasks.clear()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    day.tasks.add(task)
                }
                dayAdapter.notifyDataSetChanged()

                // Actualizar TextView con tareas
                updateTaskListTextView(day)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Error getting tasks: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchAvailableTasks(onTasksFetched: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                availableTasks.clear()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    availableTasks.add(task)
                }
                onTasksFetched()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Error getting tasks: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showSelectTaskDialog(day: Day) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.select_task_dialog, null)
        val recyclerViewTasks = dialogView.findViewById<RecyclerView>(R.id.recyclerViewTasks)
        recyclerViewTasks.layoutManager = LinearLayoutManager(context)

        val prefs = activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val groupID = prefs?.getString("group", "")

        // Obtener las tareas disponibles solo para el grupo actual
        val availableTasksFiltered =
            availableTasks.filter { task -> task.groupId == groupID }

        val selectedTaskIds = day.tasks.map { it.taskId } // Obtener los IDs de las tareas seleccionadas

        // Filtrar las tareas disponibles para mostrar solo aquellas que no están seleccionadas en el día
        val availableTasksFilteredForDay = availableTasksFiltered.filter { task ->
            !selectedTaskIds.contains(task.taskId)
        }

        val selectedTasks = mutableListOf<Task>()
        val taskShowAdapter = TaskShowAdapter(
            ArrayList(availableTasksFilteredForDay), // Convertir a ArrayList
            requireContext(),
            showCheckbox = true
        ) { task, isSelected ->
            if (isSelected) {
                selectedTasks.add(task)
            } else {
                selectedTasks.remove(task)
            }
        }
        recyclerViewTasks.adapter = taskShowAdapter

        MaterialAlertDialogBuilder(context as Context, R.style.AlertDialogTheme)
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                day.tasks.addAll(selectedTasks)
                saveTasksToFirestore(day, selectedTasks)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveTasksToFirestore(day: Day, tasks: List<Task>) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val groupID = prefs?.getString("group", "")

        val dayRef = db.collection("days").document(day.name)

        tasks.forEach { task ->
            val taskRef = dayRef.collection("tasks").document(task.name)
            // Asignar el groupId al task
            task.groupId = groupID ?: ""
            batch.set(taskRef, task)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Tarea añadida al ${day.name}", Toast.LENGTH_SHORT).show()
                dayAdapter.notifyDataSetChanged()
                fetchTasksForDay(day) // Fetch tasks again to update the view
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error añadiendo tareas: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun updateTaskListTextView(day: Day) {
        val textView = view?.findViewById<TextView>(R.id.taskListTextView)
        val taskNames = day.tasks.joinToString("\n") { it.name }
        textView?.text = taskNames
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity as Context, view)
        val inflater: MenuInflater = popup.menuInflater
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
                ?.edit()
        val intencion = Intent(context, LoginActivity::class.java)
        inflater.inflate(R.menu.options_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.log_out -> {
                    prefs?.clear()
                    prefs?.apply()

                    startActivity(intencion)

                    auth.signOut()
                    activity?.finish()
                }
            }
            true
        }
        popup.show()
    }
}
