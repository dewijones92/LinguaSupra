package com.dewijones.linguasupra.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {

    @Query("SELECT * FROM languages WHERE active = 1 ORDER BY display_order, id")
    fun observeActive(): Flow<List<Language>>

    @Query("SELECT * FROM languages ORDER BY display_order, id")
    fun observeAllIncludingInactive(): Flow<List<Language>>

    @Query("SELECT * FROM languages ORDER BY display_order, id")
    suspend fun all(): List<Language>

    @Query("SELECT * FROM languages WHERE id = :id")
    suspend fun byId(id: Long): Language?

    @Query("SELECT COALESCE(MAX(display_order), -1) FROM languages")
    suspend fun maxDisplayOrder(): Int

    @Insert
    suspend fun insert(language: Language): Long

    @Update
    suspend fun update(language: Language)

    @Query("DELETE FROM languages WHERE id = :id")
    suspend fun delete(id: Long)
}
