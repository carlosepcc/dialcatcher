package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InterceptedCallDao {
    @Query("SELECT * FROM intercepted_calls ORDER BY timestamp DESC")
    fun getAllInterceptedCalls(): Flow<List<InterceptedCall>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: InterceptedCall): Long

    @Update
    suspend fun updateCall(call: InterceptedCall)

    @Delete
    suspend fun deleteCall(call: InterceptedCall)

    @Query("DELETE FROM intercepted_calls")
    suspend fun clearAll()

    @Query("SELECT * FROM intercepted_calls WHERE id = :id LIMIT 1")
    suspend fun getCallById(id: Long): InterceptedCall?
}
