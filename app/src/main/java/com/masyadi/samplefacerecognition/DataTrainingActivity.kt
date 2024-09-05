package com.masyadi.samplefacerecognition

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import com.ahmadsuyadi.luxandfacesdk.baserecognize.CameraRecognizeActivity
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraDataTraining
import com.ahmadsuyadi.luxandfacesdk.model.DataTraining
import com.bumptech.glide.Glide
import com.masyadi.samplefacerecognition.databinding.ActivityDataTrainingBinding
import com.masyadi.samplefacerecognition.databinding.DialogConfirmImageTrainingBinding
import java.io.File
import java.util.Date

class DataTrainingActivity : CameraRecognizeActivity() {

    private lateinit var binding: ActivityDataTrainingBinding
    private lateinit var dialogConfirmImageTrainingBinding: DialogConfirmImageTrainingBinding
    private lateinit var dialog: Dialog
    private var isValidToTakePicture = false
    private var dataTraining: DataTraining? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataTraining = DataTraining("Ahmad Suyadi", null)
        setPathImageToSave(getPathImage())

        dialog = Dialog(this).apply {
            dialogConfirmImageTrainingBinding =
                DialogConfirmImageTrainingBinding.inflate(layoutInflater)
            setContentView(dialogConfirmImageTrainingBinding.root)
            with(dialogConfirmImageTrainingBinding) {
                tvName.text = dataTraining?.name
                btnCancel.setOnClickListener {
                    cancelTrainingData()
                    dismiss()
                }
                btnSave.setOnClickListener {
                    trainingData(dataTraining)
                }
            }
            setCancelable(false)
        }

        setICameraDataTraining(iCameraDataTraining)
        checkCameraPermissionsAndOpenCamera()

    }

    private val iCameraDataTraining = object : ICameraDataTraining {
        override fun onTapToTraining() {
            if (isValidToTakePicture) {
                takePicture(getPathImage())
            } else {
                cancelTrainingData()
            }

        }

        override fun onNotRecognize() {
            if (dataTraining?.recognizeID == null)
                isValidToTakePicture = true
        }

        override fun onGetResultDataTraining(recognizeID: Int) {
            dataTraining?.recognizeID = recognizeID
            dialog.dismiss()
        }

        override fun onTakePicture(outputPathImage: String?) {
            with(dialog) {
                dialogConfirmImageTrainingBinding.imageTakePicture.loadImageLocal(outputPathImage)
                show()
            }
        }

        override fun onRecognize(name: String, recognizeID: Int) {
            with(dataTraining) {
                if (this?.recognizeID != null)
                    isValidToTakePicture =
                        dataTraining?.name.equals(name) && dataTraining?.recognizeID == recognizeID
            }
        }

    }

    fun getPathImage() = "${this.applicationInfo.dataDir}/${Date().time}.jpg"
}

fun ImageView.loadImageLocal(pathImage: String?) {
    Glide.with(this.context)
        .load(File(pathImage))
        .override(100, 100)
        .centerCrop()
        .placeholder(ColorDrawable(Color.GRAY))
        .error(ColorDrawable(Color.GREEN))
        .into(this)

}