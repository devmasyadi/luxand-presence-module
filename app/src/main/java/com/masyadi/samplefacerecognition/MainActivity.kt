package com.masyadi.samplefacerecognition

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.masyadi.samplefacerecognition.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            startActivity(Intent(this, DataTrainingActivity::class.java))
        }

        binding.button2.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
    }
}