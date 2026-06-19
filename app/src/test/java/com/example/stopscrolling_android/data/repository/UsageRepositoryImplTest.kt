package com.example.stopscrolling_android.data.repository

import com.example.stopscrolling_android.data.database.UsageDao
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.worker.UploadScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class UsageRepositoryImplTest {

    @Mock
    private lateinit var usageDao: UsageDao

    @Mock
    private lateinit var uploadScheduler: UploadScheduler

    private lateinit var repository: UsageRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = UsageRepositoryImpl(usageDao, uploadScheduler)
    }

    @Test
    fun `getAllRecords returns records from dao`() = runTest {
        val records = listOf(
            UsageRecord(
                startTimeUTC = 1000L,
                endTimeUTC = 2000L,
                title = "Test",
                url = null,
                appName = "App",
                packageName = "pkg",
                category = "Cat",
                deviceName = "Test Device",
                durationSeconds = 1L,
                source = "Test"
            )
        )
        `when`(usageDao.getAllRecords()).thenReturn(flowOf(records))

        val result = repository.getAllRecords().first()

        assertEquals(records, result)
    }
}
