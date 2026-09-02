package com.example.transportapp.data.transport.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * S19 — the POD/attachment camera seam (§7.4 attachments). A picked or captured image is
 * copied out of the content provider into app-private storage, downscaled and re-compressed
 * so a 12 MP field photo becomes a few hundred KB of upload payload. Returns the relative
 * file ref (as the entities store it) and the byte size, or null when the source cannot be
 * read — the repository answers that with PHOTO_QUALITY.
 */
@Singleton
class PhotoImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun importToAppFiles(source: Uri, subdir: String): Pair<String, Long>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    ?: return@withContext null
                val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / MAX_EDGE_PX)
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = context.contentResolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@withContext null

                val dir = File(context.filesDir, subdir).apply { mkdirs() }
                val file = File(dir, "att-${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                bitmap.recycle()
                "$subdir/${file.name}" to file.length()
            }.getOrNull()
        }

    private companion object {
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 80
    }
}
