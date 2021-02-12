package com.ahmadsuyadi.luxandfacesdk.utils.extension

import android.hardware.Camera

fun Camera.turnOnFlash() {
    val parameter = parameters
    parameter.flashMode = Camera.Parameters.FLASH_MODE_TORCH
    parameters = parameter
}

fun Camera.turnOffFlash() {
    val parameter = parameters
    parameter.flashMode = Camera.Parameters.FLASH_MODE_OFF
    parameters = parameter
}
