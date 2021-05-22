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
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.File
import java.util.*

class DataTrainingActivity : CameraRecognizeActivity(), AnkoLogger {

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
                toast("Maaf anda bukan pemilik wajah ${dataTraining?.name}")
                cancelTrainingData()
            }
            info("Hallo onTapToTraining")

        }

        override fun onNotRecognize() {
            info("Hallo onNotRecognize")
            if (dataTraining?.recognizeID == null)
                isValidToTakePicture = true
        }

        override fun onGetResultDataTraining(recognizeID: Long) {
            toast("onGetResultDataTraining: $recognizeID")
            info("Hallo onGetResultDataTraining")
            dataTraining?.recognizeID = recognizeID
            dialog.dismiss()
        }

        override fun onTakePicture(outputPathImage: String?) {
            with(dialog) {
                dialogConfirmImageTrainingBinding.imageTakePicture.loadImageLocal(outputPathImage)
                show()
            }
            info("Hallo onTakePicture")
        }

        override fun onRecognize(name: String, recognizeID: Long) {
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
    pathImage?.let {
        Glide.with(this.context)
            .load(File(it))
            .override(100, 100)
            .centerCrop()
            .placeholder(ColorDrawable(Color.GRAY))
            .error(ColorDrawable(Color.GREEN))
            .into(this)
    }

}