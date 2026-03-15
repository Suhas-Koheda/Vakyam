package dev.haas.vakya

import android.app.Application

class VakyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.initialize(this)
    }
}
