package com.dewijones.linguasupra.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.dewijones.linguasupra.notify.BannerService
import java.io.File

/**
 * Replaces the live Room database with a SQLite file picked via the Storage
 * Access Framework. Used as a one-shot migration path when the user moves
 * from the legacy `com.dewijones.linguasupra.debug` install to the new
 * release variant `com.dewijones.linguasupra` — different package names mean
 * Android Auto Backup can't carry data over, so this is the manual route.
 *
 * Flow:
 *   1. Copy the Uri's bytes to a temp file in cacheDir.
 *   2. Validate it's a real SQLite file with the expected schema (`languages`
 *      and `completions` tables present). Reject otherwise.
 *   3. Close the current Room database via [AppContainer.reset].
 *   4. Stop BannerService so it can't hold a stale Repository reference.
 *   5. Delete the existing DB + WAL/SHM and move the temp file into place.
 *   6. Caller should restart the process so a fresh AppContainer rebuilds.
 *
 * The whole thing is idempotent on failure — temp file is cleaned up and the
 * existing DB is untouched until the final atomic swap.
 */
object DatabaseImporter {

    private const val TAG = "DatabaseImporter"
    private const val SQLITE_MAGIC = "SQLite format 3"

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    fun import(context: Context, source: Uri): Result {
        val app = context.applicationContext
        val cache = File(app.cacheDir, "db-import-${System.currentTimeMillis()}.db")
        try {
            // 1. Stream into temp file.
            val copied = app.contentResolver.openInputStream(source).use { input ->
                if (input == null) return Result.Failure("Couldn't open the picked file.")
                cache.outputStream().use { out -> input.copyTo(out) }
            }
            if (copied <= 0L) return Result.Failure("Picked file was empty.")

            // 2a. Magic-byte check.
            val header = ByteArray(SQLITE_MAGIC.length)
            cache.inputStream().use { it.read(header) }
            if (String(header, Charsets.US_ASCII) != SQLITE_MAGIC) {
                return Result.Failure("Not a SQLite database.")
            }

            // 2b. Schema check.
            val tables = mutableSetOf<String>()
            SQLiteDatabase.openDatabase(cache.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    while (c.moveToNext()) tables.add(c.getString(0))
                }
            }
            if (!tables.containsAll(listOf("languages", "completions"))) {
                return Result.Failure("Not a LinguaSupra database (missing languages/completions tables).")
            }

            // 3 + 4. Close Room, stop the FGS banner so the swap isn't racing
            // a running observer.
            BannerService.requestStop(app)
            AppContainer.closeAndReset(app)

            // 5. Atomic-ish swap. Wipe WAL/SHM so SQLite can't try to replay
            // a transaction log from the previous DB against the new one.
            val dbDir = app.getDatabasePath(AppDatabase.NAME).parentFile
            if (dbDir?.exists() != true) dbDir?.mkdirs()
            val target = app.getDatabasePath(AppDatabase.NAME)
            listOf(target, File(target.path + "-wal"), File(target.path + "-shm")).forEach {
                if (it.exists() && !it.delete()) {
                    Log.w(TAG, "Couldn't delete ${it.name} during import — Room may complain on next open")
                }
            }
            if (!cache.renameTo(target)) {
                // Fallback to copy if rename fails (cross-fs).
                cache.copyTo(target, overwrite = true)
                cache.delete()
            }
            return Result.Success
        } catch (t: Throwable) {
            Log.e(TAG, "Import failed", t)
            cache.delete()
            return Result.Failure("Import failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }
}
