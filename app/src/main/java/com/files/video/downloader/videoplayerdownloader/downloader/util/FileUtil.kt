package com.files.video.downloader.videoplayerdownloader.downloader.util

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileNotFoundException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import javax.inject.Inject

//@OpenForTesting
class FileUtil @Inject constructor() {

    companion object {
        var INITIIALIZED = false

        // For downloads and tmp data
        var IS_EXTERNAL_STORAGE_USE = true

        // For downloads
        var IS_APP_DATA_DIR_USE = true

        const val FOLDER_NAME = "SuperVideoDownloader"
        const val TMP_DATA_FOLDER_NAME = "video_downloader_pro_max"

        private const val KB = 1024
        private const val MB = 1024 * 1024
        private const val GB = 1024 * 1024 * 1024
        fun getFileSizeReadable(length: Double): String {

            val decimalFormat = DecimalFormat("#.##")
            return when {
                length > GB -> decimalFormat.format(length / GB) + " GB"
                length > MB -> decimalFormat.format(length / MB) + " MB"
                length > KB -> decimalFormat.format(length / KB) + " KB"
                else -> decimalFormat.format(length) + " B"
            }
        }

        fun getVideoDuration(context: Context, fileUri: Uri): Long {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, fileUri)
                val durationStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLong() ?: 0L
            } finally {
                retriever.release()
            }
        }

        fun getFileCreationDate(context: Context, fileUri: Uri): String? {
            return context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                if (dateIndex != -1 && cursor.moveToFirst()) {
                    val timestamp = cursor.getLong(dateIndex) * 1000L
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(
                            timestamp
                        )
                    )
                } else {
                    null
                }
            }
        }

        fun getFreeDiskSpace(path: File): Long {
            if (!path.exists()) {
                throw IllegalArgumentException("Path does not exist")
            }

            val stats = StatFs(path.absolutePath)
            return stats.availableBlocksLong * stats.blockSizeLong
        }

        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
    }

    val folderDir: File
        get() {
            if (!INITIIALIZED) {
                throw Error("File Util Not Initialized")
            }

            val context = ContextUtils.getApplicationContext()

            when {
                IS_EXTERNAL_STORAGE_USE && !IS_APP_DATA_DIR_USE -> {
                    return File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toURI()
                    )
                }

                IS_EXTERNAL_STORAGE_USE && IS_APP_DATA_DIR_USE -> {
                    return File(context.getExternalFilesDir(null), FOLDER_NAME)

                }

                else -> {
                    return File(context.filesDir.absolutePath, FOLDER_NAME)
                }
            }
        }

    val tmpDir: File
        get() {
            if (!INITIIALIZED) {
                throw Error("File Util Not Initialized")
            }

            val context = ContextUtils.getApplicationContext()

            return getTmpDataDir(context, IS_EXTERNAL_STORAGE_USE)
        }

    val listFiles: Map<String, Pair<Long, Uri>>
        get() {
            val context = ContextUtils.getApplicationContext()
            val result = mutableMapOf<String, Pair<Long, Uri>>()

            val externalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, true)
            val internalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, false)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val externalPublicFilesObjs = getPublicDownloadsDirFilesObjOld(context, true)
                val internalPublicFilesObjs = getPublicDownloadsDirFilesObjOld(context, false)
                result.putAll(externalPublicFilesObjs)
                result.putAll(internalPublicFilesObjs)
            } else {
                val externalPublicFilesObjsNew = getPublicDownloadsDirFilesObjNew()
                result.putAll(externalPublicFilesObjsNew)
            }
            result.putAll(externalPrivateFilesObjs)
            result.putAll(internalPrivateFilesObjs)

            return result

        }

    fun getVideoPreviewFromContent(context: Context, uri: Uri): Bitmap? {
        return try {
            if (isFileApiSupportedByUri(context, uri)) {
                return getVideoPreviewFromFile(uri.toFile())
            }

            return loadThumbnailFromMediaStore(context, uri)
        } catch (e: Throwable) {
            null
        }
    }

    private fun loadThumbnailFromMediaStore(context: Context, uri: Uri): Bitmap? {
        val videoId = getIdFromContentUri(context, uri) ?: return null
        return MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver, videoId, MediaStore.Video.Thumbnails.MINI_KIND, null
        )
    }

    private fun getVideoPreviewFromFile(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val bitmap = retriever.frameAtTime
        retriever.release()
        return bitmap
    }

    private fun getIdFromContentUri(context: Context, uri: Uri): Long? {
        // Check if the URI is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // Get the document ID
            val docId = DocumentsContract.getDocumentId(uri)
            // Split the document ID to get the last segment, which is the ID
            return docId.split(":").last().toLongOrNull()
        } else {
            // Get the ID from the last segment of the URI
            val pathSegments = uri.pathSegments

            return pathSegments.last().toLongOrNull()
        }
    }

    fun isFileWithNameNotExists(context: Context, uri: Uri, newName: String): Boolean {
        return if (isFileApiSupportedByUri(context, uri)) {
            !File(uri.toFile().parentFile, newName).exists()
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                !isDownloadedVideoContentExistsByName(context.contentResolver, uri, newName)
            } else {
                throw Exception("File api support ERROR")
            }
        }
    }

    fun moveMedia(context: Context, from: Uri, to: Uri): Boolean {
        if (isFileApiSupportedByUri(context, to)) {
//            AppLogger.d("IS_FILE_API: TRUE -- from $from to $to")
            val newFile = to.toFile()

            return from.toFile().renameTo(newFile)
        } else {
//            AppLogger.d("IS_FILE_API: FALSE -- from $from to $to")
            return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                moveFileToDownloadsFolder(
                    context.contentResolver, from.toFile(), to.toFile().name
                )
            } else {
                throw Exception("File API support ERROR!!!")
            }
        }
    }

    fun renameMedia(context: Context, from: Uri, newName: String): Pair<String, Uri>? {
        try {
            val cleanedFileName = FileNameCleaner.cleanFileName(newName) + ".mp4"
            val isNewFileNotExists = isFileWithNameNotExists(context, from, newName)

            if (cleanedFileName.isEmpty()) {
                throw Error("Empty file name")
            }

            if (!isUriExists(context, from)) {
                throw FileNotFoundException("File not found: $from")
            }

            if (!isNewFileNotExists) {
                throw Exception("File already exists")
            }
            if (isFileApiSupportedByUri(context, from)) {
                val fromFile = from.toFile()
                val toFile = File(fromFile.parentFile, cleanedFileName)
                if (toFile.exists()) {
                    throw Exception("File already exists: $toFile")
                }
                fromFile.renameTo(toFile)

                return Pair(toFile.name, Uri.fromFile(toFile))
            } else {
                val newUri = renameVideoContentFromDownloads(context, from, cleanedFileName)

                return Pair(cleanedFileName, newUri ?: from)
            }
        } catch (e: Throwable) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        }

        return null
    }

    fun renameMedia(context: Context, filePath: String, newName: String): Pair<String, String>? {
        try {
            // Làm sạch tên file và đảm bảo có đuôi .mp4
            val cleanedFileName = FileNameCleaner.cleanFileName(newName) + ".mp4"

            // Kiểm tra tên file mới có rỗng không
            if (cleanedFileName.isEmpty()) {
                throw Error("Tên file không được để trống")
            }

            val oldFile = File(filePath)

            // Kiểm tra file cũ có tồn tại không
            if (!oldFile.exists()) {
                throw FileNotFoundException("Không tìm thấy file: $filePath")
            }

            val newFile = File(oldFile.parent, cleanedFileName)

            // Kiểm tra file mới đã tồn tại chưa
            if (newFile.exists()) {
                throw Exception("File đã tồn tại: ${newFile.absolutePath}")
            }

            // Đổi tên file
            val renamed = oldFile.renameTo(newFile)
            if (renamed) {
                // Trả về tên file mới và đường dẫn đầy đủ mới
                return Pair(newFile.name, newFile.absolutePath)
            } else {
                throw Exception("Không thể đổi tên file: ${oldFile.absolutePath}")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi đổi tên file", Toast.LENGTH_SHORT).show()
        }

        return null
    }



    @Deprecated("This old")
    fun scanMedia(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            context.sendBroadcast(intent)
        } catch (e: Throwable) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMedia(context: Context, uri: Uri): Boolean {
        return try {
            if (!isUriExists(context, uri)) {
                throw FileNotFoundException("File not found: $uri")
            }
            val deleted = if (isFileApiSupportedByUri(context, uri)) {
                uri.toFile().delete()
            } else {
                deleteDownloadedVideoContent(context, uri)
            }
            deleted
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun deleteMedia(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)

            // Kiểm tra nếu file tồn tại
            if (!file.exists()) {
                Log.e("FileUtil", "File not found: $filePath")
                return false
            }

            // Nếu file là loại "content://", cần dùng contentResolver để xóa
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                val deleted = context.contentResolver.delete(uri, null, null) > 0
                Log.d("FileUtil", "Xóa file bằng contentResolver: $deleted")
                return deleted
            }

            // Xóa file trực tiếp từ hệ thống
            file.setWritable(true) // Đảm bảo có quyền ghi
            val deleted = file.delete()

            if (!deleted) {
                Log.e("FileUtil", "Không thể xóa file: $filePath")
            } else {
                Log.d("FileUtil", "File đã được xóa: $filePath")
            }

            return deleted
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi xóa file!", Toast.LENGTH_SHORT).show()
            false
        }
    }


    fun isUriExists(context: Context, uri: Uri): Boolean {
//        if (isFileApiSupportedByUri(context, uri)) {
//            return uri.toFile().exists()
//        }
//
//        try {
//            context.contentResolver.openInputStream(uri)?.close()
//        } catch (e: FileNotFoundException) {
//            return false
//        } catch (e: Exception) {
//            // Handle other exceptions as needed
//        }
//
//        // If there were no exceptions, the URI exists
//        return true

        return try {
            when (uri.scheme) {
                "file" -> File(uri.path!!).exists()
                "content" -> context.contentResolver.openInputStream(uri)?.use { true } ?: false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getContentLength(context: Context, uri: Uri): Long {
        return if (isFileApiSupportedByUri(context, uri)) {
            uri.toFile().length()
        } else {
            getContentSize(context, uri)
        }
    }

    fun isFileApiSupportedByUri(context: Context, uri: Uri): Boolean {
        val isExternalTo = isExternalUri(uri)

        val privateDir = getPrivateDownloadsDir(context, isExternalTo)
        val isAppDir = uri.toString().startsWith(Uri.fromFile(privateDir).toString())

        return !(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !isAppDir)
    }

    private fun getContentSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                -1 // Return -1 if size is unknown or an error occurred
            }
        } ?: -1 // Return -1 if the query failed
    }

    private fun renameVideoContentFromDownloads(context: Context, uri: Uri, newName: String): Uri? {
        // Check if the URI is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // Rename the document using the DocumentsContract API
            return DocumentsContract.renameDocument(context.contentResolver, uri, newName)
        } else {
            // Rename the file using the ContentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, newName)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            context.contentResolver.update(uri, values, null, null)

            return Uri.parse(uri.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isDownloadedVideoContentExistsByName(
        contentResolver: ContentResolver, contentOrig: Uri, fileName: String
    ): Boolean {
        val isExternal = isExternalUri(contentOrig)
        val contentUri = if (isExternal) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }
        // Query the Downloads collection for files with the given name
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = contentResolver.query(
            contentUri, projection, selection, selectionArgs, null
        )

        // Check if the cursor is not null and has at least one row
        val exists = (cursor?.count ?: 0) > 0

        // Close the cursor
        cursor?.close()

        return exists
    }

    private fun getTmpDataDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$TMP_DATA_FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$TMP_DATA_FOLDER_NAME"
        }

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        return file
    }

    private fun getPrivateDownloadsDirFilesObj(
        context: Context, isExternal: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        val path = getPrivateDownloadsDir(context, isExternal).absolutePath

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        val files = file.listFiles()

        if (files != null) {
            for (f in files) {
                filesMap[f.name] = Pair(f.length(), Uri.fromFile(f))
            }
        }

        return filesMap
    }

    private fun getPrivateDownloadsDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$FOLDER_NAME"
        }

        return File(path)
    }

    private fun getPublicDownloadsDirFilesObjNew(): Map<String, Pair<Long, Uri>> {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI()
        )
        val filesList =
            downloadsDir.listFiles()?.filter { it.isFile && it.extension == "mp4" }?.toTypedArray()
                ?: emptyArray<File>()
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        for (file in filesList) {
            filesMap[file.name] = Pair(file.name.hashCode().toLong(), Uri.fromFile(file))
        }

        return filesMap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getPublicDownloadsDirFilesObjOld(
        context: Context, isExternalStorage: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()
        val targetUri = if (isExternalStorage) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            targetUri,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            null,
            null, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    targetUri, id
                )

                val isUriExists = isUriExists(context, contentUri)
                if (isUriExists) {
                    filesMap[name] = Pair(id, contentUri)
                }
            }
        }

        return filesMap
    }

//    private fun deleteDownloadedVideoContent(context: Context, uri: Uri) {
//        if (DocumentsContract.isDocumentUri(context, uri)) {
//            DocumentsContract.deleteDocument(context.contentResolver, uri)
//        } else {
//            context.contentResolver.delete(uri, null, null)
//        }
//    }

    private fun deleteDownloadedVideoContent(context: Context, uri: Uri): Boolean {
        return try {
            val deletedRows = if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
                1 // Giả sử xóa thành công, do API không trả về trạng thái
            } else {
                context.contentResolver.delete(uri, null, null)
            }
            deletedRows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun isExternalUri(uri: Uri): Boolean {
        val context = ContextUtils.getApplicationContext()

        val ext1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            null
        }
        val ext2 = Uri.fromFile(context.getExternalFilesDir(null))
        val ext3 =
            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))

        val result = uri.toString().contains(ext1.toString()) || uri.toString()
            .contains(ext2.toString()) || uri.toString().contains(ext3.toString())

        return result
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveFileToDownloadsFolder(
        contentResolver: ContentResolver, sourceFile: File, fileName: String
    ): Boolean {
//        AppLogger.d(
//            "moveFileToDownloadsFoldermoveFileToDownloadsFolder $sourceFile $fileName"
//        )
        // Check if there is enough free space in the Downloads folder
        val downloadsDirectory = folderDir
        val isFolderExternal = isExternalUri(folderDir.toUri())
        val availableSpace = downloadsDirectory.freeSpace

        if (availableSpace < sourceFile.length()) {
            // Handle the case where there is not enough free space
            throw Error("Not available space $availableSpace, file size: ${sourceFile.length()}")
        }

        // Create a ContentValues object to specify the file details
        var name = fileName
        var counter = 1
        while (isDownloadExists(contentResolver, name)) {
            name = "$name($counter)"
            counter++
        }

        val cleaned = FileNameCleaner.cleanFileName(name)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cleaned)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // Insert the file into the Downloads collection
        val collectionUri = if (isFolderExternal) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }
        var fileUri = contentResolver.insert(collectionUri, values)
        if (fileUri == null) {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, cleaned.replace("mp4", "") + "_e")
            fileUri = contentResolver.insert(collectionUri, values)
        }

        // Copy the file to the Downloads folder
        val isMoved = fileUri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val copied = sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                if (copied > 0) {
//                    AppLogger.d("Source removing... $sourceFile")
                    // Delete the source file
                    sourceFile.delete()
                    true
                } else {
//                    AppLogger.d("Source move error $sourceFile")
                    false
                }
            }
        }
        return isMoved ?: false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isDownloadExists(contentResolver: ContentResolver, displayName: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(displayName)

        val uri = if (isExternalUri(folderDir.toUri())) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }
        val cursor = contentResolver.query(
            uri, projection, selection, selectionArgs, null
        )

        val exists = cursor?.moveToFirst() ?: false
        cursor?.close()

        return exists
    }
}

object FileNameCleaner {
    private const val MAX_FILE_NAME_LENGTH = 100
    private val illegalChars = intArrayOf(
        34,
        60,
        62,
        124,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        58,
        42,
        63,
        92,
        47
    )

    init {
        Arrays.sort(illegalChars)
    }

    fun cleanFileName(badFileName: String): String {
        val cleanName = StringBuilder()
        for (element in badFileName) {
            val c = element.code
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append(c.toChar())
            }
        }
        var finalName = cleanName.toString()
            .replace(".mp4", "")
            .replace("/", "").replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("`", "")
            .replace("\'", "")
            .replace("<", "")
            .replace(">", "")
            .replace(".", "_")
            .replace("|", "")
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(" ", "_").trim()
        if (finalName.isEmpty()) {
            finalName = "Untitled"
        }

        if (finalName.length > MAX_FILE_NAME_LENGTH) {
            return finalName.substring(0, MAX_FILE_NAME_LENGTH)
        }

        return finalName
    }
}