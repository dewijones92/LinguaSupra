package com.dewijones.linguasupra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Insert
    suspend fun insert(completion: Completion): Long

    @Query(
        """
        SELECT language_id AS languageId, COUNT(*) AS count
        FROM completions
        WHERE day_local_iso = :dayIso
        GROUP BY language_id
        """,
    )
    fun observeCountsForDay(dayIso: String): Flow<List<DayCount>>

    @Query(
        """
        SELECT language_id AS languageId, COUNT(*) AS count
        FROM completions
        WHERE day_local_iso = :dayIso
        GROUP BY language_id
        """,
    )
    suspend fun countsForDay(dayIso: String): List<DayCount>

    @Query(
        """
        SELECT * FROM completions
        WHERE day_local_iso = :dayIso
        ORDER BY completed_at_epoch_ms DESC
        """,
    )
    fun observeForDay(dayIso: String): Flow<List<Completion>>

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        DELETE FROM completions WHERE id = (
            SELECT id FROM completions
            WHERE language_id = :languageId AND day_local_iso = :dayIso
            ORDER BY completed_at_epoch_ms DESC, id DESC
            LIMIT 1
        )
        """,
    )
    suspend fun deleteMostRecentForDay(languageId: Long, dayIso: String): Int

    @Update
    suspend fun update(completion: Completion)

    @Query("SELECT * FROM completions ORDER BY completed_at_epoch_ms DESC")
    fun observeAll(): Flow<List<Completion>>

    @Query("SELECT * FROM completions WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Completion?

    @Query(
        """
        SELECT day_local_iso AS dayIso, language_id AS languageId, COUNT(*) AS count
        FROM completions
        WHERE day_local_iso >= :startDayIso
        GROUP BY day_local_iso, language_id
        """,
    )
    fun observeCountsSince(startDayIso: String): Flow<List<DayLanguageCount>>
}

data class DayCount(val languageId: Long, val count: Int)

data class DayLanguageCount(val dayIso: String, val languageId: Long, val count: Int)
