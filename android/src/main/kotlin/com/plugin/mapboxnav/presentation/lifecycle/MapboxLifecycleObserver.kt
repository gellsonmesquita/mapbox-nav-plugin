package com.plugin.mapboxnav.presentation.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.plugin.mapboxnav.core.utils.Logger


class MapboxLifecycleObserver : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        Logger.d("Activity onStart: Notifying MapViews")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStart()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Logger.d("Activity onStop: Notifying MapViews")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStop()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        Logger.d("Activity onCreate")
    }

    override fun onResume(owner: LifecycleOwner) {
        Logger.d("Activity onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        Logger.d("Activity onPause")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Logger.d("Activity onDestroy: Clearing all views")
        MapboxViewManager.clearAllViews()
    }
}