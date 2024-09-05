package com.masyadi.samplefacerecognition

import android.os.Bundle
import com.ahmadsuyadi.luxandfacesdk.baserecognize.CameraRecognizeActivity
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraAttendance
import com.masyadi.samplefacerecognition.databinding.ActivityAttendanceBinding

class AttendanceActivity : CameraRecognizeActivity() {

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

        }

        override fun onCloseEye() {

        }

        override fun onTakePicture(outputPathImage: String?) {

        }

        override fun onRecognize(name: String, recognizeID: Int) {

        }

    }

}