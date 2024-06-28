package com.example.gymworkout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.gymworkout.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val UserFragment = com.example.gymworkout.UserFragment()
    private val TaskFragment = com.example.gymworkout.TaskFragment()
    private val DayFragment = com.example.gymworkout.DayFragment()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val myBottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        replaceFragment(TaskFragment)

        myBottomNavigationView.selectedItemId = R.id.taskFragment



       myBottomNavigationView.setOnItemSelectedListener { item ->
           when (item.itemId) {
               R.id.userFragment -> {
                   replaceFragment(UserFragment)
                   item.title
               }
               R.id.taskFragment -> {
                   replaceFragment(TaskFragment)
                   item.title
               }
               R.id.dayFragment -> {
                   replaceFragment(DayFragment)
                   item.title
               }
               else -> Log.i("Test", "XXXXX")
           }
           true
       }
    }

    internal fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, fragment)
        transaction.commit()
    }
}