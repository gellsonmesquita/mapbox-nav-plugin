package com.plugin.mapboxnav.platform.auto


import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.plugin.mapboxnav.core.utils.Logger

class MainCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        Logger.d("Creating new car session")
        return MainCarSession()
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d("MainCarAppService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("MainCarAppService destroyed")
    }
}