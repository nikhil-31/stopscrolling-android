package com.example.stopscrolling_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UsageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<UsageRecord>)

    @Query("SELECT * FROM usage_records ORDER BY startTimeUTC DESC")
    fun getAllRecords(): Flow<List<UsageRecord>>

    @Query("SELECT * FROM usage_records WHERE startTimeUTC >= :startTime AND endTimeUTC <= :endTime ORDER BY startTimeUTC DESC")
    fun getRecordsInDateRange(startTime: Long, endTime: Long): Flow<List<UsageRecord>>

    @Query("DELETE FROM usage_records")
    suspend fun clearAllRecords()

    @Query("SELECT * FROM usage_records WHERE startTimeUTC >= :since ORDER BY startTimeUTC DESC")
    fun getRecordsSince(since: Long): Flow<List<UsageRecord>>
}
