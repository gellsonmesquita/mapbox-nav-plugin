package com.plugin.mapboxnav


import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object MapboxViewManager {

    private const val TAG = "MapboxViewManager"
    private val views: ConcurrentHashMap<Int, MapboxPlatformView> = ConcurrentHashMap()

    fun registerView(viewId: Int, view: MapboxPlatformView) {
        views[viewId] = view
        Log.d(TAG, "View com ID $viewId registrada. Total de views: ${views.size}")
    }

    fun unregisterView(viewId: Int) {
        views.remove(viewId)
        Log.d(TAG, "View com ID $viewId desregistrada. Total de views: ${views.size}")
    }

    fun getView(viewId: Int): MapboxPlatformView? {
        val view = views[viewId]
        if (view == null) {
            Log.w(TAG, "Nenhuma MapboxPlatformView encontrada para o ID: $viewId")
        }
        return view
    }

    fun getAllViews(): Collection<MapboxPlatformView> {
        return views.values
    }

    fun clearAllViews() {
        views.clear()
        Log.d(TAG, "Todas as views foram limpas.")
    }
}