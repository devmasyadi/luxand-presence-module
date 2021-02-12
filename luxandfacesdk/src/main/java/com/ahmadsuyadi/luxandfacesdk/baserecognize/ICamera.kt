package com.ahmadsuyadi.luxandfacesdk.baserecognize

interface ICamera {
    fun onTakePicture(outputPathImage: String?)
    fun onRecognize(name: String, recognizeID: Int)
}