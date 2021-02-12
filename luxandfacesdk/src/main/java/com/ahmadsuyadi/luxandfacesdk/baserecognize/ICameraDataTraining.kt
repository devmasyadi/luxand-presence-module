package com.ahmadsuyadi.luxandfacesdk.baserecognize

interface ICameraDataTraining : ICamera {
    fun onTapToTraining()
    fun onNotRecognize()
    fun onGetResultDataTraining(recognizeID: Int)
}