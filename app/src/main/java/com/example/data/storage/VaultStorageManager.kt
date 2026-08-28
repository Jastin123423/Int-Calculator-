package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import com.example.data.database.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class StorageBreakdown(
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val totalVaultBytes: Long = 0L,
    val deviceAvailableBytes: Long = 0L,
    val photoCount: Int = 0,
    val videoCount: Int = 0
)

class VaultStorageManager(private val context: Context) {

    private val vaultRootDir: File by lazy {
        File(context.filesDir, "vault").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val photosDir: File by lazy {
        File(vaultRootDir, "photos").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val videosDir: File by lazy {
        File(vaultRootDir, "videos").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val thumbnailsDir: File by lazy {
        File(vaultRootDir, "thumbnails").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private val contactsDir: File by lazy {
        File(vaultRootDir, "contacts").apply {
            if (!exists()) mkdirs()
            ensureNoMedia(this)
        }
    }

    private fun ensureNoMedia(directory: File) {
        try {
            val noMediaFile = File(directory, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        photosDir
        videosDir
        thumbnailsDir
        contactsDir
    }

    suspend fun importPhoto(uri: Uri, albumName: String = "Default"): VaultMediaEntity? = withContext(Dispatchers.IO) {
        try {
            val originalName = queryFileName(uri) ?: "IMG_${System.currentTimeMillis()}.jpg"
            val extension = originalName.substringAfterLast(".", "jpg")
            val uniqueFileName = "photo_${UUID.randomUUID()}.$extension"
            val targetFile = File(photosDir, uniqueFileName)

            var fileSize = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        fileSize += bytesRead
                    }
                    output.flush()
                }
            } ?: return@withContext null

            // Read image dimensions without loading full bitmap into memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(targetFile.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight

            VaultMediaEntity(
                fileName = originalName,
                filePath = targetFile.absolutePath,
                thumbnailPath = targetFile.absolutePath, // Coil handles thumbnail scaling efficiently
                mediaType = "PHOTO",
                fileSize = fileSize,
                width = width,
                height = height,
                albumName = albumName,
                isFavorite = false,
                isDeleted = false,
                createdTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importVideo(uri: Uri, albumName: String = "Default"): VaultMediaEntity? = withContext(Dispatchers.IO) {
        try {
            val originalName = queryFileName(uri) ?: "VID_${System.currentTimeMillis()}.mp4"
            val extension = originalName.substringAfterLast(".", "mp4")
            val uniqueFileName = "video_${UUID.randomUUID()}.$extension"
            val targetFile = File(videosDir, uniqueFileName)

            var fileSize = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        fileSize += bytesRead
                    }
                    output.flush()
                }
            } ?: return@withContext null

            // Extract metadata and generate thumbnail
            var duration = 0L
            var width = 0
            var height = 0
            var thumbnailFile: File? = null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(targetFile.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull() ?: 0L
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                width = widthStr?.toIntOrNull() ?: 0
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                height = heightStr?.toIntOrNull() ?: 0

                val frameBitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime

                if (frameBitmap != null) {
                    val thumbName = "thumb_${UUID.randomUUID()}.jpg"
                    val thumbTarget = File(thumbnailsDir, thumbName)
                    FileOutputStream(thumbTarget).use { out ->
                        frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    thumbnailFile = thumbTarget
                    frameBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }

            VaultMediaEntity(
                fileName = originalName,
                filePath = targetFile.absolutePath,
                thumbnailPath = thumbnailFile?.absolutePath ?: targetFile.absolutePath,
                mediaType = "VIDEO",
                fileSize = fileSize,
                durationMs = duration,
                width = width,
                height = height,
                albumName = albumName,
                isFavorite = false,
                isDeleted = false,
                createdTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveContactAvatar(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val uniqueName = "avatar_${UUID.randomUUID()}.jpg"
            val targetFile = File(contactsDir, uniqueName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
                val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                FileOutputStream(targetFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                if (scaled != bitmap) {
                    scaled.recycle()
                }
                bitmap.recycle()
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deletePhysicalFile(filePath: String, thumbnailPath: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            if (thumbnailPath != null && thumbnailPath != filePath) {
                val thumb = File(thumbnailPath)
                if (thumb.exists()) {
                    thumb.delete()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getStorageBreakdown(photoCount: Int, videoCount: Int): StorageBreakdown = withContext(Dispatchers.IO) {
        val photoBytes = getFolderSize(photosDir)
        val videoBytes = getFolderSize(videosDir) + getFolderSize(thumbnailsDir)
        val databaseFile = context.getDatabasePath("calcpro_database")
        val dbBytes = if (databaseFile.exists()) databaseFile.length() else 0L
        val totalVault = photoBytes + videoBytes + dbBytes

        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

        StorageBreakdown(
            photosBytes = photoBytes,
            videosBytes = videoBytes,
            databaseBytes = dbBytes,
            totalVaultBytes = totalVault,
            deviceAvailableBytes = availableBytes,
            photoCount = photoCount,
            videoCount = videoCount
        )
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getFolderSize(file) else file.length()
            }
        }
        return size
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    fun formatBytes(bytes: Long): String {
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(java.util.Locale.US, "%.1f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }
}
