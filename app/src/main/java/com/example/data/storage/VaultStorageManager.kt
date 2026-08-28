package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.data.database.VaultDocumentEntity
import com.example.data.database.VaultMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class StorageBreakdown(
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val totalVaultBytes: Long = 0L,
    val deviceAvailableBytes: Long = 0L,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val documentCount: Int = 0
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

    private val documentsDir: File by lazy {
        File(vaultRootDir, "documents").apply {
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
        documentsDir
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
                thumbnailPath = targetFile.absolutePath,
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

    suspend fun importDocument(
        uri: Uri,
        folderName: String = "Documents",
        customFileName: String? = null
    ): VaultDocumentEntity? = withContext(Dispatchers.IO) {
        try {
            val originalName = customFileName ?: queryFileName(uri) ?: "FILE_${System.currentTimeMillis()}"
            val extension = if (originalName.contains(".")) {
                originalName.substringAfterLast(".").lowercase()
            } else {
                "bin"
            }
            val uniqueFileName = "doc_${UUID.randomUUID()}.$extension"
            val targetFile = File(documentsDir, uniqueFileName)

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

            val mimeType = resolveMimeType(extension, uri)
            val category = resolveCategory(extension, mimeType)

            // Extract page count if PDF
            var pageCount = 0
            if (category == "PDF") {
                try {
                    ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        val renderer = PdfRenderer(pfd)
                        pageCount = renderer.pageCount
                        renderer.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            VaultDocumentEntity(
                fileName = originalName,
                filePath = targetFile.absolutePath,
                fileExtension = extension,
                mimeType = mimeType,
                category = category,
                fileSize = fileSize,
                folderName = folderName,
                isFavorite = false,
                isDeleted = false,
                createdTimestamp = System.currentTimeMillis(),
                modifiedTimestamp = System.currentTimeMillis(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportDocument(doc: VaultDocumentEntity, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(doc.filePath)
            if (!file.exists()) return@withContext false

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

    suspend fun getStorageBreakdown(
        photoCount: Int,
        videoCount: Int,
        docCount: Int
    ): StorageBreakdown = withContext(Dispatchers.IO) {
        val photoBytes = getFolderSize(photosDir)
        val videoBytes = getFolderSize(videosDir) + getFolderSize(thumbnailsDir)
        val docBytes = getFolderSize(documentsDir)
        val otherVaultBytes = getFolderSize(contactsDir)
        val databaseFile = context.getDatabasePath("calcpro_database")
        val dbBytes = if (databaseFile.exists()) databaseFile.length() else 0L
        val totalVault = photoBytes + videoBytes + docBytes + otherVaultBytes + dbBytes

        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

        StorageBreakdown(
            photosBytes = photoBytes,
            videosBytes = videoBytes,
            documentsBytes = docBytes,
            otherBytes = otherVaultBytes,
            databaseBytes = dbBytes,
            totalVaultBytes = totalVault,
            deviceAvailableBytes = availableBytes,
            photoCount = photoCount,
            videoCount = videoCount,
            documentCount = docCount
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

    fun queryFileName(uri: Uri): String? {
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

    fun resolveMimeType(extension: String, uri: Uri? = null): String {
        if (uri != null) {
            val type = context.contentResolver.getType(uri)
            if (!type.isNullOrBlank()) return type
        }
        val ext = extension.lowercase().removePrefix(".")
        val fromMap = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (!fromMap.isNullOrBlank()) return fromMap

        return when (ext) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt", "log" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html", "htm" -> "text/html"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "apk" -> "application/vnd.android.package-archive"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }

    fun resolveCategory(extension: String, mimeType: String): String {
        val ext = extension.lowercase().removePrefix(".")
        val mime = mimeType.lowercase()

        return when {
            ext == "pdf" || mime.contains("pdf") -> "PDF"
            ext in listOf("doc", "docx", "odt", "rtf", "pages", "wps") ||
                    mime.contains("word") || mime.contains("document") -> "DOCUMENT"
            ext in listOf("xls", "xlsx", "csv", "ods", "numbers", "tsv") ||
                    mime.contains("excel") || mime.contains("spreadsheet") || mime.contains("csv") -> "SPREADSHEET"
            ext in listOf("ppt", "pptx", "odp", "key") ||
                    mime.contains("powerpoint") || mime.contains("presentation") -> "PRESENTATION"
            ext in listOf("txt", "json", "xml", "html", "htm", "js", "ts", "kt", "java", "py", "c", "cpp", "cs", "css", "md", "log", "yaml", "yml", "ini", "sh", "conf", "sql") ||
                    mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") -> "TEXT"
            ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso") ||
                    mime.contains("zip") || mime.contains("compressed") || mime.contains("tar") -> "ARCHIVE"
            ext in listOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "wma", "opus", "mid") ||
                    mime.startsWith("audio/") -> "AUDIO"
            ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "flv", "wmv") ||
                    mime.startsWith("video/") -> "VIDEO"
            ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "heif", "ico") ||
                    mime.startsWith("image/") -> "IMAGE"
            ext in listOf("apk", "xapk", "aab") ||
                    mime.contains("vnd.android.package-archive") -> "APK"
            else -> "OTHER"
        }
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
