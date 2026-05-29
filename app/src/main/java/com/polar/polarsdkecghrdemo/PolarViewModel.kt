package com.polar.polarsdkecghrdemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class PolarViewModel(private val repository: PolarRepository) : ViewModel() {

    val connectionState = repository.connectionState
    val batteryLevel = repository.batteryLevel
    val firmwareVersion = repository.firmwareVersion

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _deviceId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val hrSharedFlow: Flow<PolarHrData> = combine(repository.readyFeatures, _deviceId) { features, id ->
        features to id
    }.flatMapLatest { (features, id) ->
        if (id != null && features.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING)) {
            _isStreaming.value = true
            repository.hrStreaming(id)
        } else {
            flowOf()
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    fun connect(deviceId: String) {
        _deviceId.value = deviceId
        repository.connect(deviceId)
    }

    fun disconnect(deviceId: String) {
        repository.disconnect(deviceId)
        _isStreaming.value = false
        _deviceId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // We might not want to shutDown here if we want it shared, 
        // but for this app's lifecycle it might be okay.
        // Actually, repository is application scoped, so we shouldn't shutDown here.
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
