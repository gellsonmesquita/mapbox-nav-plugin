package com.plugin.mapboxnav

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.util.Log
import com.mapbox.maps.MapView

class MapboxLifecycleObserver : DefaultLifecycleObserver {

    private val TAG = "MapboxLifecycleObserver"

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onStart: Notificando MapViews.")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStart()
        }
    }
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onStop: Notificando MapViews.")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStop()
        }
    }

     override fun onCreate(owner: LifecycleOwner) {  }
     override fun onResume(owner: LifecycleOwner) {  }
     override fun onPause(owner: LifecycleOwner) {  }
     override fun onDestroy(owner: LifecycleOwner) {
         MapboxViewManager.clearAllViews()
     }
}