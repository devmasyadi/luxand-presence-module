package com.masyadi.samplefacerecognition

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ahmadsuyadi.luxandfacesdk.baserecognize.CameraRecognizeActivity
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraAttendance
import com.ahmadsuyadi.luxandfacesdk.databinding.BottomMenu2Binding
import com.masyadi.samplefacerecognition.databinding.ActivityAttendanceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class AttendanceActivity : CameraRecognizeActivity(), AnkoLogger {

    private lateinit var binding: ActivityAttendanceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)

        isShowStepAttendance = true
        setICameraAttendance(iCameraAttendance)
        checkCameraPermissionsAndOpenCamera()

    }


    private val iCameraAttendance = object : ICameraAttendance {
        override fun onSmile() {
            toast("onSmile")
            info("Hallo onSmile")
        }

        override fun onCloseEye() {
            toast("onCloseEye")
            info("Hallo onCloseEye")
        }

        override fun onTakePicture(outputPathImage: String?) {
            toast("outputPathImage")
        }

        override fun onRecognize(name: String, recognizeID: Long) {
            info("Hallo onRecognize $name, $recognizeID")
        }

    }

}