package com.ahmadsuyadi.luxandfacesdk.utils.extension

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.ahmadsuyadi.luxandfacesdk.R

fun ImageView.toBottomMenuColor() {
    setColorFilter(ContextCompat.getColor(context, R.color.grey_5))
}