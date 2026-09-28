package com.coverRoot.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LogRepository {
    private const val LOG_FILE_NAME = "api.log"

    fun getLogFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    suspend fun ensureLogFileExists(context: Context) = withContext(Dispatchers.IO) {
        val file = getLogFile(context)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    suspend fun readLogLines(context: Context): List<String> = withContext(Dispatchers.IO) {
        val file = getLogFile(context)
        if (!file.exists() || file.length() == 0L) {
            emptyList()
        } else {
            file.readLines()
        }
    }
}
