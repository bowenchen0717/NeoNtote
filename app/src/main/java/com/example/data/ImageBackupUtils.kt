package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageBackupUtils {

    /**
     * Processes image URLs in a note for export/backup.
     * Local device files ("file://", "/data/...", "content://") are compressed and converted
     * to Base64 data URIs ("data:image/jpeg;base64,...") so that the backup JSON/SQL is 100% self-contained.
     * Remote URLs ("http://", "https://") and existing Base64 strings are preserved.
     */
    fun processImageUrlForExport(context: Context, imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val urls = imageUrl.split("|||").filter { it.isNotBlank() }
        if (urls.isEmpty()) return null

        val processedList = urls.map { url ->
            val trimmed = url.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:image/")) {
                trimmed
            } else {
                localImageToBase64(context, trimmed) ?: trimmed
            }
        }

        return processedList.joinToString("|||")
    }

    /**
     * Processes image URLs in a note during import/restore.
     * Base64 data URIs ("data:image/...") are decoded and written back to local storage
     * in the app's internal "filesDir/note_images/" folder, then converted to local file URIs.
     */
    fun processImageUrlForImport(context: Context, imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val urls = imageUrl.split("|||").filter { it.isNotBlank() }
        if (urls.isEmpty()) return null

        val processedList = urls.map { url ->
            val trimmed = url.trim()
            if (trimmed.startsWith("data:image/")) {
                base64ToLocalFile(context, trimmed) ?: trimmed
            } else {
                trimmed
            }
        }

        return processedList.joinToString("|||")
    }

    private fun localImageToBase64(context: Context, pathOrUri: String): String? {
        return try {
            val inputStream = when {
                pathOrUri.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(pathOrUri))
                }
                pathOrUri.startsWith("file://") -> {
                    val filePath = pathOrUri.removePrefix("file://")
                    File(filePath).takeIf { it.exists() }?.inputStream()
                }
                else -> {
                    File(pathOrUri).takeIf { it.exists() }?.inputStream()
                }
            } ?: return null

            val originalBitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null

            // Downscale to max dimension 1280px to keep JSON/SQL backup compact
            val maxDim = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val bitmap = if (width > maxDim || height > maxDim) {
                val scale = maxDim.toFloat() / Math.max(width, height)
                val newW = (width * scale).toInt()
                val newH = (height * scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            Log.e("ImageBackupUtils", "Failed to encode local image to base64: $pathOrUri", e)
            null
        }
    }

    private fun base64ToLocalFile(context: Context, base64DataUri: String): String? {
        return try {
            val commaIdx = base64DataUri.indexOf(",")
            val base64Str = if (commaIdx != -1) base64DataUri.substring(commaIdx + 1) else base64DataUri
            val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)

            val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
            val fileName = "img_restored_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val file = File(imagesDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(imageBytes)
            }
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.e("ImageBackupUtils", "Failed to decode base64 to local file", e)
            null
        }
    }
}
