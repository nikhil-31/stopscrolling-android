package com.example.stopscrolling_android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: UsageRepository
) : ViewModel() {

    val allRecords: StateFlow<List<UsageRecord>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun clearAllData() {
        repository.clearData()
    }
}
