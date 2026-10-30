package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM jarvis_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM jarvis_memory WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)

    @Query("SELECT * FROM jarvis_voice_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentVoiceLogs(): Flow<List<VoiceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceLog(log: VoiceLogEntity): Long

    @Query("DELETE FROM jarvis_voice_logs")
    suspend fun clearVoiceLogs()
}
