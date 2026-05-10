package com.dewijones.linguasupra.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dewijones.linguasupra.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Debug-only: copies the Room DB (and WAL/SHM if present) from internal
 * `filesDir` to the app's external files dir so it can be `adb pull`ed.
 *
 * Trigger: `adb shell am broadcast -n com.dewijones.linguasupra.debug/com.dewijones.linguasupra.debug.ExportDbReceiver`
 *
 * Output path on the device:
 *   /sdcard/Android/data/com.dewijones.linguasupra.debug/files/lingua-export/lingua.db
 *
 * Pull to host:
 *   adb pull /sdcard/Android/data/com.dewijones.linguasupra.debug/files/lingua-export/lingua.db
 */
class ExportDbReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext
                val dbFile = app.getDatabasePath(AppDatabase.NAME)
                val outDir = File(app.getExternalFilesDir(null), "lingua-export").apply { mkdirs() }

                // Force WAL to flush so the .db file is self-contained.
                runCatching {
                    AppDatabase.build(app).openHelper.writableDatabase
                        .query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                }.onFailure { Log.w(TAG, "wal_checkpoint failed (non-fatal)", it) }

                copyIfExists(dbFile, File(outDir, AppDatabase.NAME))
                copyIfExists(File(dbFile.parentFile, "${AppDatabase.NAME}-wal"), File(outDir, "${AppDatabase.NAME}-wal"))
                copyIfExists(File(dbFile.parentFile, "${AppDatabase.NAME}-shm"), File(outDir, "${AppDatabase.NAME}-shm"))

                Log.i(TAG, "exported DB to ${outDir.absolutePath}")
            } catch (t: Throwable) {
                Log.e(TAG, "export failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun copyIfExists(src: File, dst: File) {
        if (!src.exists()) return
        src.inputStream().use { input ->
            dst.outputStream().use { output -> input.copyTo(output) }
        }
        Log.i(TAG, "copied ${src.name} (${src.length()} bytes)")
    }

    companion object {
        private const val TAG = "ExportDbReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
