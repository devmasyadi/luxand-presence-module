package com.ahmadsuyadi.luxandfacesdk.baserecognize

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Process
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ahmadsuyadi.luxandfacesdk.R
import com.ahmadsuyadi.luxandfacesdk.databinding.BottomMenuBinding
import com.ahmadsuyadi.luxandfacesdk.databinding.TopMenuBinding
import com.ahmadsuyadi.luxandfacesdk.model.DataTraining
import com.ahmadsuyadi.luxandfacesdk.utils.ConfigLuxandFaceSDK
import com.ahmadsuyadi.luxandfacesdk.utils.camera.Preview
import com.ahmadsuyadi.luxandfacesdk.utils.camera.ProcessImageAndDrawResults
import com.ahmadsuyadi.luxandfacesdk.utils.cameraSettingIsFront
import com.ahmadsuyadi.luxandfacesdk.utils.extension.toBottomMenuColor
import com.ahmadsuyadi.luxandfacesdk.utils.extension.turnOffFlash
import com.ahmadsuyadi.luxandfacesdk.utils.extension.turnOnFlash
import com.ahmadsuyadi.luxandfacesdk.utils.setCameraSetting
import com.luxand.FSDK
import com.luxand.FSDK.*
import org.jetbrains.anko.AnkoLogger


open class CameraRecognizeActivity : AppCompatActivity(), AnkoLogger {

    lateinit var bottomMenu: BottomMenuBinding
    lateinit var topMenu: TopMenuBinding
    private var mIsFailed = false
    private var wasStopped = false
    private var isFrontCamera = true
    private var isTurnOnFlash = false
    private var mPreview: Preview? = null
    private var mDraw: ProcessImageAndDrawResults? = null
    private var iCameraDataTraining: ICameraDataTraining? = null
    private var iCameraAttendance: ICameraAttendance? = null
    private var database: String? = null
    private var pathImageToSave = ""
    private var mLayout: FrameLayout? = null
    var isShowStepAttendance = false
    var sDensity = 1.0f

    fun setICameraDataTraining(iCameraDataTraining: ICameraDataTraining) {
        this.iCameraDataTraining = iCameraDataTraining
    }

    fun setICameraAttendance(iCameraAttendance: ICameraAttendance) {
        this.iCameraAttendance = iCameraAttendance
    }

    fun setDatabase(database: String) {
        this.database = database
    }

    fun setPathImageToSave(path: String) {
        pathImageToSave = path
    }

    private fun showErrorAndClose(error: String, code: Int) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("$error: $code")
            .setPositiveButton(
                "Ok"
            ) { _, _ -> Process.killProcess(Process.myPid()) }
            .show()
    }

    private fun resetTrackerParameters() {
        val errpos = IntArray(1)
        SetTrackerMultipleParameters(
            mDraw!!.mTracker,
            "ContinuousVideoFeed=true;FacialFeatureJitterSuppression=0;RecognitionPrecision=1;Threshold=0.996;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=2000;HandleArbitraryRotations=false;DetermineFaceRotationAngle=false;InternalResizeWidth=70;FaceDetectionThreshold=3;",
            errpos
        )
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters, position", errpos[0])
        }
        SetTrackerMultipleParameters(
            mDraw!!.mTracker,
            "DetectGender=false;DetectExpression=true",
            errpos
        )
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters 2, position", errpos[0])
        }

        // faster smile detection
        SetTrackerMultipleParameters(
            mDraw!!.mTracker,
            "AttributeExpressionSmileSmoothingSpatial=0.5;AttributeExpressionSmileSmoothingTemporal=10;",
            errpos
        )
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters 3, position", errpos[0])
        }
    }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFrontCamera = cameraSettingIsFront()
        sDensity = resources.displayMetrics.scaledDensity
        val res = ActivateLibrary(ConfigLuxandFaceSDK.licenseKey)
        if (res != FSDK.FSDKE_OK) {
            mIsFailed = true
            showErrorAndClose("FaceSDK activation failed", res)
        } else {
            Initialize()
            // Lock orientation
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mLayout = FrameLayout(this)
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            mLayout?.layoutParams = params
            setContentView(mLayout)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> openCamera()
            else -> {
                finish()
            }
        }
    }

    fun checkCameraPermissionsAndOpenCamera() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                val onCloseAlert = Runnable {
                    ActivityCompat.requestPermissions(
                        this@CameraRecognizeActivity, arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
                alert(this, onCloseAlert, "The application processes frames from camera.")
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        // Camera layer and drawing layer
        val background = View(this)
        background.setBackgroundColor(Color.BLACK)
        mDraw = ProcessImageAndDrawResults(this)
        mDraw?.sDensity = sDensity
        mDraw?.iCameraAttendance = iCameraAttendance
        mDraw?.iCameraDataTraining = iCameraDataTraining
        mDraw?.pathImageToSave = pathImageToSave
        mPreview = Preview(this, mDraw)
        //mPreview.setBackgroundColor(Color.GREEN);
        //mDraw.setBackgroundColor(Color.RED);
        mDraw?.mTracker = HTracker()
        if (FSDK.FSDKE_OK != LoadTrackerMemoryFromFile(mDraw?.mTracker, database)) {
            val res = CreateTracker(mDraw?.mTracker)
            if (FSDK.FSDKE_OK != res) {
                showErrorAndClose("Error creating tracker", res)
            }
        }
        resetTrackerParameters()
        this.window.setBackgroundDrawable(ColorDrawable()) //black background
        mLayout?.visibility = View.VISIBLE
        addContentView(
            background,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        addContentView(
            mPreview,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        ) //creates MainActivity contents
        addContentView(
            mDraw,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        // menu
        bottomMenu = BottomMenuBinding.inflate(layoutInflater)
        with(bottomMenu) {
            imgFlipCamera.toBottomMenuColor()
            imgFlash.toBottomMenuColor()
            imgTakePicture.toBottomMenuColor()
            imgFlipCamera.setOnClickListener {
                flipCamera()
            }
            imgFlash.setOnClickListener {
                toggleFlash()
            }
        }
        addContentView(
            bottomMenu.root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        if(isShowStepAttendance) {
            topMenu = TopMenuBinding.inflate(layoutInflater)
            addContentView(
                topMenu.root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

    }

    override fun onStop() {
        stopCamera()
        super.onStop()
    }

    private fun flipCamera() {
        stopCamera()
        isFrontCamera = !isFrontCamera
        setCameraSetting(isFrontCamera)
        openCamera()
    }

    private fun toggleFlash() {
        isTurnOnFlash = !isTurnOnFlash
        with(bottomMenu) {
            if (isTurnOnFlash) {
                mPreview?.mCamera?.turnOnFlash()
                imgFlash.setImageResource(R.drawable.ic_baseline_flash_on_24)
            } else {
                mPreview?.mCamera?.turnOffFlash()
                imgFlash.setImageResource(R.drawable.ic_baseline_flash_off_24)
            }
        }
    }

    private fun stopCamera() {
        if (mDraw != null || mPreview != null) {
            mPreview!!.visibility = View.GONE // to destroy surface
            mLayout!!.visibility = View.GONE
            mLayout!!.removeAllViews()
            mPreview!!.releaseCallbacks()
            mPreview = null
            mDraw = null
            wasStopped = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (wasStopped && mDraw == null) {
            checkCameraPermissionsAndOpenCamera()
            //openCamera();
            wasStopped = false
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mDraw != null) {
            pauseProcessingFrames()
            saveTrackerMemory()
        }
    }

    private fun saveTrackerMemory() {
        SaveTrackerMemoryToFile(mDraw!!.mTracker, database)
    }

    fun takePicture(pathImageToSave: String) {
        this.pathImageToSave = pathImageToSave
        mDraw?.takePicture(pathImageToSave)
    }

    public override fun onResume() {
        super.onResume()
        if (mIsFailed) return
        resumeProcessingFrames()
    }

    private fun pauseProcessingFrames() {
        if (mDraw != null) {
            mDraw!!.mStopping = 1

            // It is essential to limit wait time, because mStopped will not be set to 0, if no frames are feeded to mDraw
            for (i in 0..99) {
                if (mDraw!!.mStopped != 0) break
                try {
                    Thread.sleep(10)
                } catch (ex: Exception) {
                }
            }
        }
    }

    fun trainingData(dataTraining: DataTraining?) {
        dataTraining?.let {
            mDraw?.trainingData(it)
            saveTrackerMemory()
        }
    }

    fun cancelTrainingData() {
        mDraw?.cancelTrainingData()
    }

    private fun resumeProcessingFrames() {
        if (mDraw != null) {
            mDraw!!.mStopped = 0
            mDraw!!.mStopping = 0
        }
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
        var sDensity = 1.0f
        fun alert(context: Context?, callback: Runnable?, message: String?) {
            val dialog: AlertDialog.Builder = AlertDialog.Builder(context!!)
            dialog.setMessage(message)
            dialog.setNegativeButton(
                "Ok"
            ) { dialog, _ -> dialog.dismiss() }
            if (callback != null) {
                dialog.setOnDismissListener { callback.run() }
            }
            dialog.show()
        }
    }
}

