package com.kduniv.aimong.feature.chat

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatImageSaver {

    fun saveDataUriImage(context: Context, dataUri: String): Result<String> {
        return runCatching {
            val (mimeType, bytes) = decodeDataUri(dataUri)
            val extension = extensionForMime(mimeType)
            val displayName = buildFileName(extension)
            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/AImong",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("insert failed")

            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
            } ?: throw IllegalStateException("openOutputStream failed")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val published = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, published, null, null)
            }

            displayName
        }
    }

    private fun decodeDataUri(dataUri: String): Pair<String, ByteArray> {
        val trimmed = dataUri.trim()
        val commaIndex = trimmed.indexOf(',')
        require(commaIndex > 0) { "invalid data uri" }

        val header = trimmed.substring(0, commaIndex)
        val payload = trimmed.substring(commaIndex + 1)
        val mimeType = header
            .removePrefix("data:")
            .substringBefore(';')
            .trim()
            .ifEmpty { "image/png" }

        val bytes = Base64.decode(payload, Base64.DEFAULT)
        require(bytes.isNotEmpty()) { "empty image" }

        return mimeType to bytes
    }

    private fun extensionForMime(mimeType: String): String = when (mimeType.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "png"
    }

    private fun buildFileName(extension: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "AImong_$stamp.$extension"
    }
}
