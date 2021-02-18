package com.masyadi.samplefacerecognition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ahmadsuyadi.luxandfacesdk.utils.ConfigLuxandFaceSDK
import com.masyadi.samplefacerecognition.databinding.ActivityMainBinding
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            startActivity<DataTrainingActivity>()
        }

        binding.button2.setOnClickListener {
            startActivity<AttendanceActivity>()
        }
    }
}