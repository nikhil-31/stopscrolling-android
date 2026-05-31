package com.example.stopscrolling_android.di

import android.content.Context
import androidx.room.Room
import com.example.stopscrolling_android.data.database.AppDatabase
import com.example.stopscrolling_android.data.database.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stopscrolling_db"
        ).build()
    }

    @Provides
    fun provideUsageDao(database: AppDatabase): UsageDao {
        return database.usageDao()
    }
}
