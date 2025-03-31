package com.example.facedetector

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner

data class detectorstate(
    val image: Bitmap? = null,
    val lensFacing = CameraSelector.LENS_FACING_BACK,
    var imageWidth by remember { mutableStateOf(1) },
    var imageHeight by remember { mutableStateOf(1) },


)
