package com.plugin.mapboxnav.infrastructure.registry

import android.os.Build
import androidx.annotation.RequiresApi
import com.plugin.mapboxnav.domain.utils.Logger
import com.plugin.mapboxnav.presentation.views.MapboxPlatformView
import java.util.concurrent.ConcurrentHashMap

object MapboxViewManager {

    private val views: ConcurrentHashMap<Int, MapboxPlatformView> = ConcurrentHashMap()

    fun registerView(viewId: Int, view: MapboxPlatformView) {
        views[viewId] = view
        Logger.d("View registered with ID $viewId. Total views: ${views.size}")
    }

    fun unregisterView(viewId: Int) {
        views.remove(viewId)
        Logger.d("View unregistered with ID $viewId. Total views: ${views.size}")
    }

    fun getView(viewId: Int): MapboxPlatformView? {
        val view = views[viewId]
        if (view == null) {
            Logger.w("No MapboxPlatformView found for ID: $viewId")
        }else {
            registerView(viewId, view)
        }
        return view
    }

    fun getAllViews(): Collection<MapboxPlatformView> {
        return views.values
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun clearAllViews() {
        views.values.forEach { it.dispose() }
        views.clear()
        Logger.d("All views cleared")
    }

    fun getViewCount(): Int {
        return views.size
    }
}