package com.example.teminavigator.domain

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Loads, imports and deletes the map image shown in the app.
 *
 * The map is either the bundled default (`assets/map.png`) or a custom PNG
 * imported by the user into the app's private storage. The path of a custom map
 * is kept in [com.example.teminavigator.data.AppSettings.mapImagePath]. All file work runs on [dispatcher], so
 * the suspending functions are safe to call from the main thread.
 *
 * @param context Any Android context; the application context is used internally.
 * @param dispatcher Dispatcher for file operations. Defaults to [Dispatchers.IO]
 * and can be replaced in tests.
 */
class MapLoader(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** Application context, kept instead of the passed one to avoid leaking an Activity. */
    private val appContext = context.applicationContext

    /**
     * Constants of the loader.
     *
     * - `ASSET_NAME`: file name of the bundled default map in the assets folder.
     * - `MAX_FILE_BYTES`: largest accepted import size (20 MB).
     * - `MAX_DIMENSION`: largest accepted width and height of an imported map, in pixels.
     */
    companion object {
        private const val ASSET_NAME = "map.png"
        private const val MAX_FILE_BYTES = 20L * 1024 * 1024
        private const val MAX_DIMENSION = 4096
    }

    /**
     * Loads the map image, falling back to the bundled default.
     *
     * The custom file is used if [path] is set and the file exists and can be
     * decoded. Otherwise, for example because no path was saved or the file was
     * deleted, the bundled map is loaded.
     *
     * @param path Path of a custom map file, or null to use the default.
     * @return The map as an [ImageBitmap], ready to draw.
     * @throws java.io.IOException if the bundled default map is missing from the assets.
     */
    suspend fun load(path: String?): ImageBitmap =
        withContext(dispatcher) {
            path?.let { decodeFile(File(it)) } ?: decodeAsset()
        }

    /**
     * Imports a user-picked PNG as the new custom map.
     *
     * The file is first copied to a temporary file and checked: it must be a
     * non-empty PNG of at most [MAX_FILE_BYTES] with width and height between 1
     * and [MAX_DIMENSION] pixels. If valid, it is moved to a unique file name in
     * the app's private storage and the previous custom map is deleted. If
     * anything fails, nothing is changed and the previous map stays in use. The
     * temporary file is always removed.
     *
     * @param uri Location of the picked image, e.g. from the system file picker.
     * @param previousPath Path of the currently used custom map, which is deleted
     * after a successful import, or null if there is none.
     * @return Absolute path of the imported map, or null if the import failed or
     * the file was invalid. Save the returned path with
     * [com.example.teminavigator.data.SettingsRepository.setMapImagePath].
     */
    suspend fun import(
        uri: Uri,
        previousPath: String?,
    ): String? =
        withContext(dispatcher) {
            val tmp = File(appContext.filesDir, "map_import.tmp")
            val target = File(appContext.filesDir, "map_${System.currentTimeMillis()}.png")
            try {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                } ?: return@withContext null

                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tmp.path, opts)
                val valid =
                    tmp.length() in 1..MAX_FILE_BYTES &&
                        opts.outMimeType == "image/png" &&
                        opts.outWidth in 1..MAX_DIMENSION &&
                        opts.outHeight in 1..MAX_DIMENSION
                if (!valid || !tmp.renameTo(target)) return@withContext null

                previousPath?.let { File(it).delete() }
                target.absolutePath
            } catch (_: Exception) {
                null
            } finally {
                val deleted = tmp.delete()
                Log.i("MapLoader", "Maploader object delete result: $deleted")
            }
        }

    /**
     * Deletes a custom map file, e.g. when resetting to the default map.
     *
     * Does nothing if [path] is null or the file doesn't exist. Not suspending, so
     * call it from a background thread. The saved path in settings must be
     * cleared separately.
     *
     * @param path Path of the file to delete, or null.
     */
    fun deleteLocal(path: String?) {
        path?.let { File(it).delete() }
    }

    /** Decodes a map file. Returns null if the file doesn't exist or can't be decoded, which triggers the fallback in [load]. */
    private fun decodeFile(file: File): ImageBitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path)?.asImageBitmap() else null

    /** Decodes the bundled default map from the assets folder. */
    private fun decodeAsset(): ImageBitmap =
        appContext.assets
            .open(ASSET_NAME)
            .use { BitmapFactory.decodeStream(it) }
            .asImageBitmap()
}
