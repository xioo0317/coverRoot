package com.coverRoot.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads the update APK and launches the system installer.
 *
 * The downloaded file is written into the app's external files dir and exposed
 * through a [FileProvider] (`${applicationId}.fileprovider`) so the package
 * installer can read it.
 */
object ApkInstaller {

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    /**
     * Downloads [url] to `update/coverRoot-update.apk`, reporting progress via
     * [onProgress] (0..100). Returns the local file on success.
     */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val total = body.contentLength()
                val dir = File(context.getExternalFilesDir(null), "update").apply { mkdirs() }
                // Clear previous artifacts.
                dir.listFiles()?.forEach { it.delete() }
                val target = File(dir, "coverRoot-update.apk")
                target.outputStream().use { output ->
                    val source = body.byteStream()
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Long = 0
                    while (true) {
                        val read = source.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (total > 0) {
                            onProgress((bytesRead * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                    output.flush()
                }
                if (target.exists() && target.length() > 0) target else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Launches the system package installer for [apk]. */
    fun install(context: Context, apk: File): Result = try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        Result.Success
    } catch (e: Exception) {
        Result.Failure(e.message ?: "install error")
    }
}
