package com.example.stopscrolling_android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the local upload outbox.
 *
 * In the server-authoritative model this table is *not* permanent history: every
 * captured session lives here only until the backend confirms its upload, at which
 * point the row is deleted. Timelines and insights are rendered from the backend
 * sessions API, never from this table. The rows that remain are, by definition,
 * pending uploads.
 */
@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UsageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<UsageRecord>)

    /** Pending (not-yet-uploaded) outbox rows, newest first. Used to overlay freshly
     *  captured activity on top of server data before it has been uploaded. */
    @Query("SELECT * FROM usage_records ORDER BY startTimeUTC DESC")
    fun getAllRecords(): Flow<List<UsageRecord>>

    @Query("DELETE FROM usage_records")
    suspend fun clearAllRecords()

    @Query("SELECT * FROM usage_records ORDER BY endTimeUTC ASC LIMIT :limit")
    suspend fun getUnsyncedRecords(limit: Int): List<UsageRecord>

    @Query("SELECT COUNT(*) FROM usage_records")
    suspend fun getUnsyncedCount(): Int

    /** Deletes outbox rows once the backend has confirmed their upload. */
    @Query("DELETE FROM usage_records WHERE id IN (:ids)")
    suspend fun deleteRecordsByIds(ids: List<String>)
}
