package com.ahmadsuyadi.luxandfacesdk.utils

import java.io.File

fun Int.isValidConfidenceSmile() = this > ConfigLuxandFaceSDK.minimumConfidenceSmile
fun Int.isValidConfidenceEyesOpen() = this < ConfigLuxandFaceSDK.minimumConfidenceEysOpen
fun String.deleteFile() = File(this).delete()