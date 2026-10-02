package me.weishu.kernelsu.ui.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo
import okhttp3.Request

/**
 * @author weishu
 * @date 2023/6/22.
 */
/** Extract a safe file name from a download URL. */
fun fileNameFromUrl(url: String): String {
    val raw = android.net.Uri.decode(url.substringAfterLast('/').substringBefore('?'))
    return raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "coverRoot-update.apk" }
}

suspend fun download(
    url: String,
    fileName: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {}
) {
    onDownloading()

    val downloadId = DownloadManager.enqueue(
        context = ksuApp,
        url = url,
        fileName = fileName,
        onCompleted = onDownloaded,
    )

    DownloadManager.downloads
        .onEach { map -> map[downloadId]?.let { onProgress(it.progress) } }
        .first { map ->
            val status = map[downloadId]?.status
            status == DownloadManager.Status.COMPLETED ||
                status == DownloadManager.Status.FAILED
        }
}

internal suspend fun isDownloadAvailable(uri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ksuApp.contentResolver.openFileDescriptor(uri, "r").use { it != null }
    }.getOrDefault(false)
}

fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(ksuApp)) return LatestVersionInfo()
    val url = "http://49.233.30.172/update.json"
    runCatching {
        ksuApp.okhttpClient.newCall(Request.Builder().url(url).build()).execute()
            .use { response ->
                if (!response.isSuccessful) {
                    return LatestVersionInfo()
                }
                val json = org.json.JSONObject(response.body.string())
                val versionCode = json.optInt("versionCode", 0)
                val zipUrl = json.optString("zipUrl")
                val changelogUrl = json.optString("changelog")
                val changelog = if (changelogUrl.isNotEmpty()) {
                    runCatching {
                        ksuApp.okhttpClient.newCall(Request.Builder().url(changelogUrl).build())
                            .execute().use { changelogResp ->
                                if (changelogResp.isSuccessful) changelogResp.body.string() else ""
                            }
                    }.getOrDefault("")
                } else ""
                return LatestVersionInfo(versionCode, zipUrl, changelog)
            }
    }
    return LatestVersionInfo()
}
