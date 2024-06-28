package com.example.gymworkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymworkout.adaptadores.TaskShowAdapter
import com.example.gymworkout.databinding.CreatetaskDialogBinding
import com.example.gymworkout.databinding.FragmentTaskBinding
import com.example.gymworkout.entidades.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class TaskFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var taskArrayList: ArrayList<Task>
    private lateinit var taskShowAdapter: TaskShowAdapter
    private var showCheckbox: Boolean = false

    private lateinit var taskName: String
    private lateinit var taskRepetitions: String
    private lateinit var taskSets: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // Shared preferences para obtener el email
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs?.getString("email", null)
        val groupID = prefs?.getString("group", "")

        auth = Firebase.auth

        taskArrayList = arrayListOf()

        taskShowAdapter =
            TaskShowAdapter(taskArrayList, requireContext(), showCheckbox) { task, isSelected ->
                // Callback para la selección de tareas
            }

        binding.recyclerViewUserShow.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
            adapter = taskShowAdapter
        }

        binding.menuButtonTasks.setOnClickListener {
            showPopup(binding.menuButtonTasks)
        }

        binding.floatingAddButton.setOnClickListener {
            taskShowAdapter =
                TaskShowAdapter(taskArrayList, requireContext(), showCheckbox) { _, _ ->
                    // Callback para la selección de tareas
                }
            binding.recyclerViewUserShow.adapter = taskShowAdapter
            taskShowAdapter.notifyDataSetChanged()

            db.collection("users").document(email as String).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.getBoolean("groupAdmin") == true) {
                        val taskDialog =
                            CreatetaskDialogBinding.inflate(layoutInflater, binding.root, false)

                        MaterialAlertDialogBuilder(context as Context, R.style.AlertDialogTheme)
                            .setView(taskDialog.root)
                            .setTitle(getString(R.string.create_Task))
                            .setIcon(R.drawable.ic_baseline_assignment_24)
                            .setNegativeButton(getString(R.string.cancelar_tarea)) { _, _ ->
                            }
                            .setPositiveButton(getString(R.string.guardar_Tarea)) { _, _ ->
                                taskName = taskDialog.taskNameText.text.toString().trim()
                                taskRepetitions =
                                    taskDialog.taskRepetitionsText.text.toString().trim()
                                taskSets = taskDialog.taskSetsText.text.toString().trim()
                                createTask(groupID)
                                showCheckbox = false // Restablecer a false después de agregar una tarea
                                taskShowAdapter =
                                    TaskShowAdapter(
                                        taskArrayList,
                                        requireContext(),
                                        showCheckbox
                                    ) { _, _ ->
                                    }
                                binding.recyclerViewUserShow.adapter = taskShowAdapter
                                taskShowAdapter.notifyDataSetChanged()
                            }
                            .show()
                    } else {
                        Snackbar.make(view, getString(R.string.onlyAdminCan), Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
        }

        calldb()
    }

    private fun calldb() {
        db = FirebaseFirestore.getInstance()

        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val groupID = prefs?.getString("group", "")

        db.collection("tasks").whereEqualTo("groupId", groupID)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.i("Carencias en firestore", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {
                        if (dc.type == DocumentChange.Type.ADDED) {
                            var task = dc.document.toObject(Task::class.java)
                            task.taskId = dc.document.id
                            if (taskArrayList.indexOf(task) == -1) {
                                taskArrayList.add(task)
                                taskShowAdapter.notifyItemInserted(taskArrayList.size)
                            }
                        }
                        // Si se edita una tarea
                        if (dc.type == DocumentChange.Type.MODIFIED) {
                            var newTask = dc.document.toObject(Task::class.java)
                            newTask.taskId = dc.document.id

                            var position: Int = 0
                            for (task in taskArrayList) {
                                if (task.taskId == dc.document.id) {
                                    position = taskArrayList.indexOf(task)
                                    taskArrayList[position] = newTask
                                    taskShowAdapter.notifyItemChanged(position)
                                    taskShowAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            })
    }

    private fun showPopup(view: View) {
        val popup = PopupMenu(activity as Context, view)
        val inflater: MenuInflater = popup.menuInflater
        val prefs =
            activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
                ?.edit()
        inflater.inflate(R.menu.options_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.log_out -> {
                    prefs?.clear()
                    prefs?.apply()
                    goLoginPage()
                    auth.signOut()
                    activity?.finish()
                }
            }
            true
        }
        popup.show()
    }

    fun goLoginPage() {
        val intent = Intent(context, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun View.removeDialog() {
        this ?: return
        val parentView = parent as? ViewGroup ?: return
        parentView.removeView(this)
    }

    private fun createTask(groupId: String?) {
        var taskId = UUID.randomUUID().toString()
        if (taskName != "" && taskRepetitions != "" && taskSets != "") {
            db.collection("tasks").document(taskId).set(
                hashMapOf(
                    "taskId" to taskId,
                    "name" to taskName,
                    "repetitions" to taskRepetitions,
                    "sets" to taskSets,
                    "groupId" to groupId
                )
            ).addOnSuccessListener {
                Toast.makeText(context, getString(R.string.taskCreateSucces), Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            Toast.makeText(context, getString(R.string.taskCreateFail), Toast.LENGTH_LONG).show()
        }
    }
}
