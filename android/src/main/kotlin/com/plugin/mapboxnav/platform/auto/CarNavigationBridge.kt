package com.plugin.mapboxnav.platform.auto

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.plugin.mapboxnav.core.utils.Logger
import com.plugin.mapboxnav.presentation.controllers.NavigationController

class CarNavigationBridge(private val context: Context) {

    private var navigationController: NavigationController? = null
    private var isCarSessionActive = false

    fun initialize(lifecycleOwner: LifecycleOwner) {
        navigationController = NavigationController(context).apply {
            initialize(lifecycleOwner)
        }
        Logger.d("CarNavigationBridge initialized")
    }

    fun onCarSessionStarted() {
        isCarSessionActive = true
        Logger.d("Car session started")
    }

    fun onCarSessionEnded() {
        isCarSessionActive = false
        Logger.d("Car session ended")
    }

    fun getNavigationController(): NavigationController? {
        return navigationController
    }

    fun isCarActive(): Boolean {
        return isCarSessionActive
    }

    fun cleanup() {
        navigationController?.cleanup()
        navigationController = null
        isCarSessionActive = false
        Logger.d("CarNavigationBridge cleaned up")
    }

    companion object {
        @Volatile
        private var instance: CarNavigationBridge? = null

        fun getInstance(context: Context): CarNavigationBridge {
            return instance ?: synchronized(this) {
                instance ?: CarNavigationBridge(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}