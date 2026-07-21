package com.dennyfid.golfswingrecorder.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.dennyfid.golfswingrecorder.motion.MotionDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.dennyfid.golfswingrecorder.motion.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val motionDetector: MotionDetector
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onMotionDetected: (Double) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(surfaceProvider.hashCode()) // Placeholder for rotation
                .build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val mat = ImageUtils.imageProxyToMat(imageProxy)
                    val score = motionDetector.detectMotion(mat)
                    onMotionDetected(score)
                    mat.release()
                } catch (e: Exception) {
                    Log.e("CameraManager", "Analysis error", e)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording(onEvent: (VideoRecordEvent) -> Unit) {
        val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GolfSwingRecorder")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture?.output
            ?.prepareRecording(context, mediaStoreOutputOptions)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(context), onEvent)
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }
}
