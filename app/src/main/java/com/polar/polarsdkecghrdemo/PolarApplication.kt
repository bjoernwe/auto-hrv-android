package com.polar.polarsdkecghrdemo

import android.app.Application

import com.polar.polarsdkecghrdemo.data.repository.PolarRepository

class PolarApplication : Application() {
    val repository: PolarRepository by lazy {
        PolarRepository(this)
    }
}
