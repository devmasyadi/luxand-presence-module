package com.masyadi.samplefacerecognition

import android.os.Bundle
import com.ahmadsuyadi.luxandfacesdk.baserecognize.CameraRecognizeActivity
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraAttendance
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class AttendanceActivity : CameraRecognizeActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        setICameraAttendance(iCameraAttendance)
        checkCameraPermissionsAndOpenCamera()

    }

    private val iCameraAttendance = object : ICameraAttendance {
        override fun onSmile() {
            toast("onSmile")
        }

        override fun onCloseEye() {
            toast("onCloseEye")
        }

        override fun onTakePicture(outputPathImage: String?) {
            toast("outputPathImage")
        }

        override fun onRecognize(name: String, recognizeID: Int) {
            info("Hallo onRecognize $name, $recognizeID")
        }

    }

}