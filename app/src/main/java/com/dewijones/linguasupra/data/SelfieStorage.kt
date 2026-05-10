package com.dewijones.linguasupra.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Stores selfie photos under `filesDir/selfies/` (internal, app-private).
 * Files are timestamped so historic captures are preserved on disk for a future
 * "selfie diary" feature; the current displayed avatar is tracked separately
 * via [UserPreferences.selfiePath].
 */
object SelfieStorage {
    private const val DIR = "selfies"
    private val FILE_NAME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
    private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    fun newSelfieFile(context: Context, prefix: String = "selfie"): File {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val stamp = LocalDateTime.now().format(FILE_NAME_FMT)
        return File(dir, "${prefix}-$stamp.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX,
            file,
        )
}
