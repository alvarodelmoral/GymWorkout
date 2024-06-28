package com.example.gymworkout.entidades


data class Task(
    var taskId: String = "",
    var name: String = "",
    var sets: String = "",
    var repetitions: String = "",
    var groupId : String = "",
    var isSelected: Boolean = false
)

