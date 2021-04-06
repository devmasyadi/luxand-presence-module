package com.ahmadsuyadi.luxandfacesdk.utils

import com.luxand.FSDK

object LuxandUtils {

    fun updateName(id: Long, name: String, templatePath: String) {
        val tracker = FSDK.HTracker()
        FSDK.LockID(tracker, id)
        FSDK.SetName(tracker, id, name)
        FSDK.UnlockID(tracker, id)
        FSDK.SaveTrackerMemoryToFile(tracker, templatePath)
    }

    fun deleteUser(id: Long, templatePath: String) {
        val tracker = FSDK.HTracker()
        FSDK.PurgeID(tracker, id)
        FSDK.SaveTrackerMemoryToFile(tracker, templatePath)
    }

}