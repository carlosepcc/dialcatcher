package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intercepted_calls")
data class InterceptedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawUri: String,
    val decodedCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // e.g., "CALL", "DIAL", "MANUAL"
    val notes: String = ""
)
