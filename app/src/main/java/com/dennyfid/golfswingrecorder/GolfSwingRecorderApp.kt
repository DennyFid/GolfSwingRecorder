package com.dennyfid.golfswingrecorder

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class GolfSwingRecorderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV loaded successfully")
        } else {
            Log.e("OpenCV", "OpenCV load failed")
        }
    }
}
