package com.example.facedetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Composable
fun CameraPreviewScreen() {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = Preview.Builder().build()
    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    val previewView = remember { PreviewView(context) }
    val handLandmarkerHelper = HandLandmarkerHelper(
        runningMode = RunningMode.LIVE_STREAM,  // Use IMAGE mode for single bitmap processing
        context = context,
        handLandmarkerHelperListener = object : HandLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String, errorCode: Int) {
                // Handle errors here
                Log.e("HandLandmarker", "Error: $error")
            }

            override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                val handLandmarkerResults = resultBundle.results.firstOrNull()
                handLandmarkerResults?.let { result ->
                    if (result.landmarks().isNotEmpty()) {
                        val landmarks = result.landmarks()[0].map { landmark ->
                            PointF(
                                landmark.x() * resultBundle.inputImageWidth,
                                landmark.y() * resultBundle.inputImageHeight
                            )
                        }

                        // Do something with these landmarks
                    }
                }
                // Do something with the landmarks
            }
        }
    )



    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalyzer = ImageAnalysis.Builder()
//            .setTargetResolution(Size(320, 320))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(context)
                ) { imageProxy ->
                    try {
                        handLandmarkerHelper.detectLiveStream(
                            imageProxy,
                            isFrontCamera = false
                        )

                    } catch (e: Exception) {
                        Log.e("ImageAnalysis", "Error converting image", e)
                    }
                }
            }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraxSelector,
            preview,
            imageCapture,
            imageAnalyzer
        )

    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

    }
}


suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }







