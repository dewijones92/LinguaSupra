package com.dewijones.linguasupra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}

data class DayCount(val languageId: Long, val count: Int)
