package bme.prompteng.android.climbtracker.ui.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun copyUriToFile(context: Context, uri: Uri): File {
    val mimeType = context.contentResolver.getType(uri)
    val extension = if (mimeType?.startsWith("video/") == true) ".mp4" else ".jpg"

    val tempFile = File(context.cacheDir, "gallery_media_${System.currentTimeMillis()}$extension")

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("Failed to open input stream for Uri: $uri")
    } catch (e: Exception) {
        throw Exception("Error copying Uri to file: ${e.message}")
    }
    
    if (!tempFile.exists() || tempFile.length() == 0L) {
        throw Exception("Copied file is empty or does not exist.")
    }

    return tempFile
}

fun persistUriToInternalStorage(context: Context, uri: Uri): File {
    val mimeType = context.contentResolver.getType(uri)
    val extension = if (mimeType?.startsWith("video/") == true) ".mp4" else ".jpg"
    
    val directory = File(context.filesDir, "chat_images")
    if (!directory.exists()) directory.mkdirs()

    val file = File(directory, "chat_media_${System.currentTimeMillis()}$extension")

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("Failed to open input stream for Uri: $uri")
    } catch (e: Exception) {
        throw Exception("Error persisting Uri to internal storage: ${e.message}")
    }
    
    return file
}
