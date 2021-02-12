package com.ahmadsuyadi.luxandfacesdk.utils

import android.content.Context

fun Context.setCameraSetting(isFrontCamera: Boolean) {
    val sharedPref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putBoolean(Constants.KEY_IS_FRONT_CAMERA, isFrontCamera)
        apply()
    }
}

fun Context.cameraSettingIsFront(): Boolean {
    val sharedPref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    return sharedPref.getBoolean(Constants.KEY_IS_FRONT_CAMERA, true)
}