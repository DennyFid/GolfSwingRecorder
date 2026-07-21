package com.dennyfid.golfswingrecorder.motion

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer

object ImageUtils {
    fun imageProxyToMat(image: ImageProxy): Mat {
        val nv21 = yuv420ToNv21(image)
        val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        yuvMat.put(0, 0, nv21)
        val rgbMat = Mat()
        org.opencv.imgproc.Imgproc.cvtColor(yuvMat, rgbMat, org.opencv.imgproc.Imgproc.COLOR_YUV2RGB_NV21)
        return rgbMat
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}
