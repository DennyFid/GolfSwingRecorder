package com.dennyfid.golfswingrecorder.motion

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotionDetector @Inject constructor() {

    private var previousFrame: Mat? = null
    private var threshold = 25.0
    private var minMotionArea = 500.0

    fun detectMotion(currentFrame: Mat): Double {
        // Define ROI: Middle third of the image
        val width = currentFrame.width()
        val height = currentFrame.height()
        val roi = Rect(width / 3, 0, width / 3, height)
        val frameRoi = currentFrame.submat(roi)

        val gray = Mat()
        Imgproc.cvtColor(frameRoi, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(21.0, 21.0), 0.0)

        if (previousFrame == null || previousFrame?.size() != gray.size()) {
            previousFrame = gray
            return 0.0
        }

        val frameDelta = Mat()
        Core.absdiff(previousFrame, gray, frameDelta)
        val thresh = Mat()
        Imgproc.threshold(frameDelta, thresh, threshold, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.dilate(thresh, thresh, Mat(), Point(-1.0, -1.0), 2)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var maxArea = 0.0
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                maxArea = area
            }
        }

        previousFrame = gray
        return maxArea
    }

    fun setThreshold(value: Double) {
        threshold = value
    }

    fun reset() {
        previousFrame = null
    }
}
