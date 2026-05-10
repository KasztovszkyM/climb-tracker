package bme.prompteng.android.climbtracker.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageOptimizer {

    // We downscaling the image because ML models require lower resolution anyway.
    // Increasing to 1600 to help the AI detect very small holds (chips/jibs).
    fun compressAndResizeImage(context: Context, originalFile: File, maxWidth: Int = 1600): File {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(originalFile.absolutePath, options)

        // Maintain aspect ratio
        val ratio = options.outWidth.toFloat() / options.outHeight.toFloat()
        val targetWidth = if (options.outWidth > maxWidth) maxWidth else options.outWidth
        val targetHeight = (targetWidth / ratio).toInt()

        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

        val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        // Create a new optimized file in the cache directory
        val compressedFile = File(context.cacheDir, "optimized_wall_${System.currentTimeMillis()}.jpg")
        FileOutputStream(compressedFile).use { out ->
            // 80% quality is plenty for computer vision and drastically reduces file size
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        // Free up memory
        bitmap.recycle()
        if (bitmap !== resizedBitmap) resizedBitmap.recycle()

        return compressedFile
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}