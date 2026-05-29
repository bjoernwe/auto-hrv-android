package com.polar.polarsdkecghrdemo

import android.app.Application

class PolarApplication : Application() {
    val repository: PolarRepository by lazy {
        PolarRepository(this)
    }
}
