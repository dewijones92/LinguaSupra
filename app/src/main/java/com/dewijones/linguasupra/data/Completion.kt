package com.dewijones.linguasupra.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = Language::class,
            parentColumns = ["id"],
            childColumns = ["language_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("language_id"), Index("day_local_iso")],
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "language_id") val languageId: Long,
    @ColumnInfo(name = "completed_at_epoch_ms") val completedAtEpochMs: Long,
    @ColumnInfo(name = "day_local_iso") val dayLocalIso: String,
)
