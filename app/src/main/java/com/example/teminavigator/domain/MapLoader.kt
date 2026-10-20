package com.example.teminavigator.domain

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MapLoader(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        private const val ASSET_NAME = "map.png"
        private const val MAX_FILE_BYTES = 20L * 1024 * 1024
        private const val MAX_DIMENSION = 4096
    }

    /** Loading with fallback to the bundled asset. */
    suspend fun load(path: String?): ImageBitmap = withContext(Dispatchers.IO) {
        path?.let { decodeFile(File(it)) } ?: decodeAsset()
    }

    /** Copies the picked PNG into private storage. Returns the new path, or null if invalid. */
    suspend fun import(uri: Uri, previousPath: String?): String? = withContext(Dispatchers.IO) {
        val tmp = File(appContext.filesDir, "map_import.tmp")
        val target = File(appContext.filesDir, "map_${System.currentTimeMillis()}.png")
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tmp.path, opts)
            val valid = tmp.length() in 1..MAX_FILE_BYTES &&
                    opts.outMimeType == "image/png" &&
                    opts.outWidth in 1..MAX_DIMENSION &&
                    opts.outHeight in 1..MAX_DIMENSION
            if (!valid || !tmp.renameTo(target)) return@withContext null

            previousPath?.let { File(it).delete() }
            target.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            tmp.delete()
        }
    }

    fun deleteLocal(path: String?) { path?.let { File(it).delete() } }

    private fun decodeFile(file: File): ImageBitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null

    private fun decodeAsset(): ImageBitmap =
        appContext.assets.open(ASSET_NAME).use { BitmapFactory.decodeStream(it) }.asImageBitmap()
}