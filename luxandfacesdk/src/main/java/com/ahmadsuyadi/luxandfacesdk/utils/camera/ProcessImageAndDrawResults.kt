package com.ahmadsuyadi.luxandfacesdk.utils.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.ahmadsuyadi.luxandfacesdk.baserecognize.CameraRecognizeActivity
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraAttendance
import com.ahmadsuyadi.luxandfacesdk.baserecognize.ICameraDataTraining
import com.ahmadsuyadi.luxandfacesdk.model.DataTraining
import com.ahmadsuyadi.luxandfacesdk.utils.cameraSettingIsFront
import com.ahmadsuyadi.luxandfacesdk.utils.deleteFile
import com.ahmadsuyadi.luxandfacesdk.utils.isValidConfidenceEyesOpen
import com.ahmadsuyadi.luxandfacesdk.utils.isValidConfidenceSmile
import com.luxand.FSDK
import com.luxand.FSDK.GetTrackerFacialAttribute
import com.luxand.FSDK.GetValueConfidence
import org.jetbrains.anko.AnkoLogger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.pow

// Draw graphics on top of the video
class ProcessImageAndDrawResults(context: Context) :
    View(context), AnkoLogger {
    var mTracker: FSDK.HTracker? = null
    private val MAX_FACES = 5
    private val mFacePositions = arrayOfNulls<FaceRectangle>(MAX_FACES)
    private val mIDs = LongArray(MAX_FACES)
    private val faceLock: Lock = ReentrantLock()
    private var mTouchedIndex: Int
    private var mTouchedID: Long = 0
    var mStopping: Int
    var mStopped: Int
    private var mContext: Context
    private var mPaintGreen: Paint
    private var mPaintBlue: Paint
    private var mPaintBlueTransparent: Paint
    var mYUVData: ByteArray?
    var mRGBData: ByteArray?
    var mImageWidth = 0
    var mImageHeight = 0
    private var first_frame_saved: Boolean
    var rotated: Boolean
    var iCameraDataTraining: ICameraDataTraining? = null
    var iCameraAttendance: ICameraAttendance? = null
    var pathImageToSave: String? = null
    private var confidenceSmilePercent = arrayOfNulls<Int>(MAX_FACES)
    private var confidenceEyesOpenPercent = arrayOfNulls<Int>(MAX_FACES)
    private var isTakePicture = false

    private fun getFaceFrame(Features: FSDK.FSDK_Features?, fr: FaceRectangle?): Int {
        if (Features == null || fr == null) return FSDK.FSDKE_INVALID_ARGUMENT
        val u1 = Features.features[0]!!.x.toFloat()
        val v1 = Features.features[0]!!.y.toFloat()
        val u2 = Features.features[1]!!.x.toFloat()
        val v2 = Features.features[1]!!.y.toFloat()
        val xc = (u1 + u2) / 2
        val yc = (v1 + v2) / 2
        val w =
            ((u2 - u1) * (u2 - u1) + (v2 - v1) * (v2 - v1)).toDouble().pow(0.5).toInt()
        fr.x1 = (xc - w * 1.6 * 0.9).toInt()
        fr.y1 = (yc - w * 1.1 * 0.9).toInt()
        fr.x2 = (xc + w * 1.6 * 0.9).toInt()
        fr.y2 = (yc + w * 2.1 * 0.9).toInt()
        if (fr.x2 - fr.x1 > fr.y2 - fr.y1) {
            fr.x2 = fr.x1 + fr.y2 - fr.y1
        } else {
            fr.y2 = fr.y1 + fr.x2 - fr.x1
        }
        return 0
    }

    override fun onDraw(canvas: Canvas) {
        if (mStopping == 1) {
            mStopped = 1
            super.onDraw(canvas)
            return
        }
        if (mYUVData == null || mTouchedIndex != -1 && !isTakePicture) {
            super.onDraw(canvas)
            return  //nothing to process or name is being entered now
        }
        val canvasWidth = canvas.width
        //int canvasHeight = canvas.getHeight();

        // Convert from YUV to RGB
        decodeYUV420SP(
            mRGBData,
            mYUVData!!, mImageWidth, mImageHeight
        )

        // Load image to FaceSDK
        val Image = FSDK.HImage()
        val imagemode = FSDK.FSDK_IMAGEMODE()
        imagemode.mode = FSDK.FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT
        FSDK.LoadImageFromBuffer(
            Image,
            mRGBData,
            mImageWidth,
            mImageHeight,
            mImageWidth * 3,
            imagemode
        )
        FSDK.MirrorImage(Image, !context.cameraSettingIsFront())
        val rotatedImage = FSDK.HImage()
        FSDK.CreateEmptyImage(rotatedImage)

        //it is necessary to work with local variables (onDraw called not the time when mImageWidth,... being reassigned, so swapping mImageWidth and mImageHeight may be not safe)
        var ImageWidth = mImageWidth
        //int ImageHeight = mImageHeight;
        if (rotated) {
            ImageWidth = mImageHeight
            //ImageHeight = mImageWidth;
            FSDK.RotateImage90(Image, -1, rotatedImage)
        } else {
            FSDK.CopyImage(Image, rotatedImage)
        }
        FSDK.FreeImage(Image)

        // Save first frame to gallery to debug (e.g. rotation angle)
        /*
		if (!first_frame_saved) {
			first_frame_saved = true;
			String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
			FSDK.SaveImageToFile(RotatedImage, galleryPath + "/first_frame.jpg"); //frame is rotated!
		}
		*/

        if (isTakePicture) {
            isTakePicture = false
            FSDK.SaveImageToFile(rotatedImage, pathImageToSave)
            iCameraDataTraining?.onTakePicture(pathImageToSave)
            iCameraAttendance?.onTakePicture(pathImageToSave)
        }

        val IDs = LongArray(MAX_FACES)
        val face_count = LongArray(1)
        FSDK.FeedFrame(mTracker, 0, rotatedImage, face_count, IDs)
        FSDK.FreeImage(rotatedImage)
        faceLock.lock()
        for (i in 0 until MAX_FACES) {
            mFacePositions[i] = FaceRectangle()
            mFacePositions[i]!!.x1 = 0
            mFacePositions[i]!!.y1 = 0
            mFacePositions[i]!!.x2 = 0
            mFacePositions[i]!!.y2 = 0
            mIDs[i] = IDs[i]
        }
        val ratio = canvasWidth * 1.0f / ImageWidth
        for (i in 0 until face_count[0].toInt()) {
            val Eyes = FSDK.FSDK_Features()
            FSDK.GetTrackerEyes(mTracker, 0, mIDs[i], Eyes)
            getFaceFrame(Eyes, mFacePositions[i])
            mFacePositions[i]!!.x1 *= ratio.toInt()
            mFacePositions[i]!!.y1 *= ratio.toInt()
            mFacePositions[i]!!.x2 *= ratio.toInt()
            mFacePositions[i]!!.y2 *= ratio.toInt()

            val values = arrayOfNulls<String>(1)
            GetTrackerFacialAttribute(mTracker, 0, IDs[i], "Expression", values, 1024)
            val confidenceSmile = FloatArray(1)
            val confidenceEyesOpen = FloatArray(1)
            GetValueConfidence(values[0], "Smile", confidenceSmile)
            GetValueConfidence(values[0], "EyesOpen", confidenceEyesOpen)
            confidenceSmilePercent[i] = (confidenceSmile[0] * 100).toInt()
            confidenceEyesOpenPercent[i] = (confidenceEyesOpen[0] * 100).toInt()

        }
        faceLock.unlock()
        val shift = (22 * CameraRecognizeActivity.sDensity).toInt()

        // Mark and name faces
        for (i in 0 until face_count[0]) {
            canvas.drawRect(
                mFacePositions[i.toInt()]!!.x1.toFloat(),
                mFacePositions[i.toInt()]!!.y1.toFloat(),
                mFacePositions[i.toInt()]!!.x2.toFloat(),
                mFacePositions[i.toInt()]!!.y2.toFloat(),
                mPaintBlueTransparent
            )
            var named = false
            if (IDs[i.toInt()] != (-1).toLong()) {
                val names = arrayOfNulls<String>(1)
                FSDK.GetAllNames(mTracker, IDs[i.toInt()], names, 1024)
                if (names[0] != null && names[0]!!.isNotEmpty()) {
                    iCameraDataTraining?.let {
                        it.onRecognize(names[0]!!, IDs[i.toInt()].toInt())
                        canvas.drawText(
                            "Tap to training",
                            ((mFacePositions[i.toInt()]!!.x1 + mFacePositions[i.toInt()]!!.x2) / 2).toFloat(),
                            (mFacePositions[i.toInt()]!!.y2 + shift).toFloat(),
                            mPaintGreen
                        )
                    }
                    iCameraAttendance?.let {
                        it.onRecognize(names[0]!!, IDs[i.toInt()].toInt())
                        if (confidenceEyesOpenPercent[i.toInt()]?.isValidConfidenceEyesOpen() == false &&
                            confidenceSmilePercent[i.toInt()]?.isValidConfidenceSmile() == true
                        ) {
                            it.onSmile()
                        }
                        if (confidenceEyesOpenPercent[i.toInt()]?.isValidConfidenceEyesOpen() == true &&
                            confidenceSmilePercent[i.toInt()]?.isValidConfidenceSmile() == false
                        ) {
                            it.onCloseEye()
                        }
                        canvas.drawText(
                            "${names[0]!!} smile: ${confidenceSmilePercent[i.toInt()]}, eyesOpen: ${confidenceEyesOpenPercent[i.toInt()]}",
                            ((mFacePositions[i.toInt()]!!.x1 + mFacePositions[i.toInt()]!!.x2) / 2).toFloat(),
                            (mFacePositions[i.toInt()]!!.y2 + shift).toFloat(),
                            mPaintGreen
                        )
                    }
                    named = true
                }
            }
            if (!named) {
                iCameraDataTraining?.let {
                    it.onNotRecognize()
                    canvas.drawText(
                        "Tap to training",
                        ((mFacePositions[i.toInt()]!!.x1 + mFacePositions[i.toInt()]!!.x2) / 2).toFloat(),
                        (mFacePositions[i.toInt()]!!.y2 + shift).toFloat(),
                        mPaintGreen
                    )
                }
                iCameraAttendance?.let {
                    canvas.drawText(
                        "Unknown",
                        ((mFacePositions[i.toInt()]!!.x1 + mFacePositions[i.toInt()]!!.x2) / 2).toFloat(),
                        (mFacePositions[i.toInt()]!!.y2 + shift).toFloat(),
                        mPaintGreen
                    )
                }
            }
        }
        super.onDraw(canvas)
    } // end onDraw method

    override fun onTouchEvent(event: MotionEvent): Boolean { //NOTE: the method can be implemented in Preview class
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                iCameraDataTraining?.let {
                    val x = event.x.toInt()
                    val y = event.y.toInt()
                    faceLock.lock()
                    val rects = arrayOfNulls<FaceRectangle>(MAX_FACES)
                    val IDs = LongArray(MAX_FACES)
                    run {
                        var i = 0
                        while (i < MAX_FACES) {
                            rects[i] = FaceRectangle()
                            rects[i]!!.x1 = mFacePositions[i]!!.x1
                            rects[i]!!.y1 = mFacePositions[i]!!.y1
                            rects[i]!!.x2 = mFacePositions[i]!!.x2
                            rects[i]!!.y2 = mFacePositions[i]!!.y2
                            IDs[i] = mIDs[i]
                            ++i
                        }
                    }
                    faceLock.unlock()
                    var i = 0
                    while (i < MAX_FACES) {
                        if (rects[i] != null && rects[i]!!.x1 <= x && x <= rects[i]!!.x2 && rects[i]!!.y1 <= y && y <= rects[i]!!.y2 + 30) {
                            mTouchedID = IDs[i]
                            mTouchedIndex = i
                            it.onTapToTraining()
                            break
                        }
                        ++i
                    }
                }
            }
        }
        return true
    }

    fun trainingData(dataTraining: DataTraining) {
        val id = dataTraining.recognizeID?.toLong() ?: mTouchedID
        FSDK.LockID(mTracker, id)
        FSDK.SetName(mTracker, id, dataTraining.name)
        FSDK.UnlockID(mTracker, id)
        mTouchedIndex = -1
        iCameraDataTraining?.onGetResultDataTraining(id.toInt())
    }

    fun cancelTrainingData() {
        mTouchedIndex = -1
        pathImageToSave?.deleteFile()
    }

    fun takePicture(pathImageToSave: String) {
        this.pathImageToSave = pathImageToSave
        isTakePicture = true
    }

    companion object {
        fun decodeYUV420SP(rgb: ByteArray?, yuv420sp: ByteArray, width: Int, height: Int) {
            val frameSize = width * height
            var yp = 0
            for (j in 0 until height) {
                var uvp = frameSize + (j shr 1) * width
                var u = 0
                var v = 0
                for (i in 0 until width) {
                    var y = (0xff and yuv420sp[yp].toInt()) - 16
                    if (y < 0) y = 0
                    if (i and 1 == 0) {
                        v = (0xff and yuv420sp[uvp++].toInt()) - 128
                        u = (0xff and yuv420sp[uvp++].toInt()) - 128
                    }
                    val y1192 = 1192 * y
                    var r = y1192 + 1634 * v
                    var g = y1192 - 833 * v - 400 * u
                    var b = y1192 + 2066 * u
                    if (r < 0) r = 0 else if (r > 262143) r = 262143
                    if (g < 0) g = 0 else if (g > 262143) g = 262143
                    if (b < 0) b = 0 else if (b > 262143) b = 262143
                    rgb!![3 * yp] = (r shr 10 and 0xff).toByte()
                    rgb[3 * yp + 1] = (g shr 10 and 0xff).toByte()
                    rgb[3 * yp + 2] = (b shr 10 and 0xff).toByte()
                    ++yp
                }
            }
        }
    }

    init {
        mTouchedIndex = -1
        mStopping = 0
        mStopped = 0
        rotated = false
        mContext = context
        mPaintGreen = Paint()
        mPaintGreen.style = Paint.Style.FILL
        mPaintGreen.color = Color.GREEN
        mPaintGreen.textSize = 18 * CameraRecognizeActivity.sDensity
        mPaintGreen.textAlign = Paint.Align.CENTER
        mPaintBlue = Paint()
        mPaintBlue.style = Paint.Style.FILL
        mPaintBlue.color = Color.BLUE
        mPaintBlue.textSize = 18 * CameraRecognizeActivity.sDensity
        mPaintBlue.textAlign = Paint.Align.CENTER
        mPaintBlueTransparent = Paint()
        mPaintBlueTransparent.style = Paint.Style.STROKE
        mPaintBlueTransparent.strokeWidth = 2f
        mPaintBlueTransparent.color = Color.BLUE
        mPaintBlueTransparent.textSize = 25f

        //mBitmap = null;
        mYUVData = null
        mRGBData = null
        first_frame_saved = false
    }
} // end of ProcessImageAndDrawResults class

