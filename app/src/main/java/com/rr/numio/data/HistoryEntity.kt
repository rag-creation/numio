package com.rr.numio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,   // e.g. "4+2"
    val result: String,       // e.g. "6"
    val timestamp: Long = System.currentTimeMillis()
)

