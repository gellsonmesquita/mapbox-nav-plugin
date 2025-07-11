package com.app.mapbox_nav_plugin

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.util.Log
import com.mapbox.maps.MapView

/**
 * Observador de ciclo de vida da Activity que notifica todas as instâncias de MapboxPlatformView
 * sobre eventos de ciclo de vida para que elas possam gerenciar seus MapViews internos.
 */
class MapboxLifecycleObserver : DefaultLifecycleObserver {

    private val TAG = "MapboxLifecycleObserver"

    // Chamado quando a Activity entra no estado STARTED (visível para o usuário)
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onStart: Notificando MapViews.")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStart() // Chama onStart no MapView de cada instância
        }
    }

    // Chamado quando a Activity entra no estado STOPPED (não visível para o usuário)
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Activity onStop: Notificando MapViews.")
        MapboxViewManager.getAllViews().forEach { platformView ->
            platformView.mapView?.onStop() // Chama onStop no MapView de cada instância
        }
    }

    // Você pode adicionar outros métodos de ciclo de vida aqui, se necessário:
    // override fun onCreate(owner: LifecycleOwner) { ... }
    // override fun onResume(owner: LifecycleOwner) { ... }
    // override fun onPause(owner: LifecycleOwner) { ... }
    // override fun onDestroy(owner: LifecycleOwner) {
    //     Log.d(TAG, "Activity onDestroy: Limpando todas as views (se não forem já descartadas individualmente).")
    //     MapboxViewManager.clearAllViews() // Opcional: pode ser útil para garantir limpeza final
    // }
}