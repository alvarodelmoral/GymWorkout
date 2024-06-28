package com.example.gymworkout.entidades

data class Day(
    val name: String,
    val tasks: MutableList<Task> = mutableListOf()
)
