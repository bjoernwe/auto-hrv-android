package com.polar.polarsdkecghrdemo.ui.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polar.polarsdkecghrdemo.data.repository.PolarRepository
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class PolarViewModel(private val repository: PolarRepository) : ViewModel() {

    val connectionState = repository.connectionState
    val batteryLevel = repository.batteryLevel
    val firmwareVersion = repository.firmwareVersion
    val isStreaming = repository.isStreaming
    val deviceId = PolarRepository.DEVICE_ID

    val hrSharedFlow: Flow<PolarHrData.PolarHrSample> = repository.hrFlow
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    fun connect() {
        repository.connect()
    }

    fun disconnect() {
        repository.disconnect()
    }

    class Factory(private val repository: PolarRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PolarViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PolarViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
