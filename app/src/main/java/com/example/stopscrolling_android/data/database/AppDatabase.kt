package com.example.stopscrolling_android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UsageRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}
