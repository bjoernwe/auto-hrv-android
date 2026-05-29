package com.polar.polarsdkecghrdemo.data.model

import com.polar.sdk.api.model.PolarDeviceInfo

sealed class ConnectionState {
    object Idle : ConnectionState()
    data class Connecting(val deviceId: String) : ConnectionState()
    data class Connected(val deviceInfo: PolarDeviceInfo) : ConnectionState()
    data class Disconnected(val deviceId: String) : ConnectionState()
}
