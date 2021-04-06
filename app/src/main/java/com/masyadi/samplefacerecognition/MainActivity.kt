package com.masyadi.samplefacerecognition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ahmadsuyadi.luxandfacesdk.utils.ConfigLuxandFaceSDK
import com.ahmadsuyadi.luxandfacesdk.utils.LuxandUtils
import com.masyadi.samplefacerecognition.databinding.ActivityMainBinding
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = this.applicationInfo.dataDir + "/ahmadsuyadi.dat"

        binding.button.setOnClickListener {
            startActivity<DataTrainingActivity>()
        }

        binding.button2.setOnClickListener {
            startActivity<AttendanceActivity>()
        }

        with(binding) {
            btnEditName.setOnClickListener {
                LuxandUtils.updateName(1, edtName.text.toString(), database)
            }
            btnDelete.setOnClickListener {
                LuxandUtils.deleteUser(1, database)
            }
        }
    }
}