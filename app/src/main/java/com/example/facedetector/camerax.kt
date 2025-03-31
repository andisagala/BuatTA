package com.example.facedetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.example.facedetector.OverlayView
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult


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
//    val overlayView = OverlayView()
    var handLandmarkerResult by remember { mutableStateOf<HandLandmarkerResult?>(null) }

//    val hasilresults =

    val previewView = remember { PreviewView(context) }
//    val overlayview = remember{OverlayView(context)}
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }


    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(
            runningMode = RunningMode.LIVE_STREAM,
            context = context,
            handLandmarkerHelperListener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    // Handle errors here
                    Log.e("HandLandmarker", "Error: $error")
                }

                override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
                    val handLandmarkerResults = resultBundle.results.firstOrNull()
                    handLandmarkerResults?.let { result ->
//                        if (result.landmarks().isNotEmpty()) {
                            handLandmarkerResult = handLandmarkerResults
//                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            // Or use RATIO_16_9_FALLBACK_AUTO_STRATEGY for 16:9
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
//            .setTargetResolution(Size(320, 320))
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setResolutionSelector(resolutionSelector)
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
                        imageWidth = imageProxy.width
                        imageHeight = imageProxy.height

                    } catch (e: Exception) {
                        Log.e("ImageAnalysis", "Error converting image", e)
                    }
                    finally {
                        imageProxy.close() // Ensure imageProxy is closed
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

    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerHelper.clearHandLandmarker()
        }
    }
//    val a = 1

    HandLandmarkerView(
        previewView = previewView,
        handLandmarkerResult= handLandmarkerResult,
        imageWidth = imageWidth,
        imageHeight = imageHeight
    )



}


suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

@Composable
fun HandLandmarkerView(
    previewView: PreviewView,
    handLandmarkerResult: HandLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int
) {

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

//        val b = 1



        // Landmarks overlay
        if (handLandmarkerResult != null) {
//            val a = 1
//            if (handLandmarkerResult.handednesses().size == 0)
//            {
//                Canvas(modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Transparent)){
//                    drawRect(color = Color.Transparent)
//                }
//            }
//            else
            Canvas(modifier = Modifier.fillMaxSize()) {

                val scaleFactor = minOf(
                    size.width / imageWidth.toFloat(),
                    size.height / imageHeight.toFloat()
                )

                val scaleX = size.width / imageWidth.toFloat()
                val scaleY = size.height / imageHeight.toFloat()




                // Draw landmarks
                handLandmarkerResult.landmarks().forEach { landmarks ->
                    // Draw points
                    landmarks.forEach { landmark ->
                        val x = landmark.x() * imageWidth * scaleX
                        val y = landmark.y() * imageHeight * scaleY

                        drawCircle(
                            color = Color.Yellow,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }

                    // Draw connections
                    HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
                        val startLandmark = landmarks[connection.start()]
                        val endLandmark = landmarks[connection.end()]

                        drawLine(
                            color = Color(0xFFBB86FC), // Purple 200
                            start = Offset(
                                startLandmark.x() * imageWidth * scaleX,
                                startLandmark.y() * imageHeight * scaleY
                            ),
                            end = Offset(
                                endLandmark.x() * imageWidth * scaleX,
                                endLandmark.y() * imageHeight * scaleY
                            ),
                            strokeWidth = 8f
                        )
                    }
                }
            }



        }

    }
}






