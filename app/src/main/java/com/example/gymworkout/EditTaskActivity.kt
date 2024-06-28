package com.example.gymworkout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.gymworkout.databinding.ActivityEditTaskBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class EditTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditTaskBinding
    private lateinit var db: FirebaseFirestore

    private lateinit var taskId: String
    private lateinit var name: String
    private lateinit var repetitions: String
    private lateinit var groupId: String
    private lateinit var sets: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = FirebaseFirestore.getInstance()

        taskId = intent.extras?.getString("taskId").toString().trim()
        name = intent.extras?.getString("name").toString().trim()
        repetitions = intent.extras?.getString("repetitions").toString().trim()
        sets = intent.extras?.getString("sets").toString().trim()
        groupId = intent.extras?.getString("groupId").toString().trim()



        binding.nameEditTaskText.setText(name)
        binding.nameEditTaskSetsText.setText(sets)
        binding.nameEditTaskRepetitionsText.setText(repetitions)

        binding.botonCancelarEditarTarea.setOnClickListener {
            finish()
        }

        binding.botonEditarTarea.setOnClickListener {
            updateTask(groupId, it)
        }
    }

    private fun updateTask(groupId: String?, view : View) {
        var nameedit = binding.nameEditTaskText.text.toString().trim()
        var repetitionsedit = binding.nameEditTaskRepetitionsText.text.toString().trim()
        var setsedit = binding.nameEditTaskSetsText.text.toString().trim()

        if (nameedit != "" && repetitionsedit != "" && setsedit != "") {
            db.collection("tasks").document(taskId).set(
                hashMapOf(
                    "taskId" to taskId,
                    "name" to nameedit,
                    "repetitions" to repetitionsedit,
                    "sets" to setsedit,
                    "groupId" to groupId
                )
            ).addOnSuccessListener {
                finish()
            }
        } else {
            Snackbar.make(view, "ERROR AL ACTUALIZAR", Snackbar.LENGTH_LONG).show()
        }

    }

}