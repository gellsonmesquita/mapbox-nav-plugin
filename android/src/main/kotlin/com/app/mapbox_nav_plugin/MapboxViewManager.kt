package com.app.mapbox_nav_plugin

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerencia as instâncias de MapboxPlatformView associadas a seus respectivos viewIds.
 * Garante que cada chamada de método do Flutter possa ser direcionada à instância correta da view.
 */
object MapboxViewManager {

    private const val TAG = "MapboxViewManager"
    private val views: ConcurrentHashMap<Int, MapboxPlatformView> = ConcurrentHashMap()

    /**
     * Registra uma nova instância de MapboxPlatformView.
     * @param viewId O ID único da view da plataforma.
     * @param view A instância de MapboxPlatformView a ser registrada.
     */
    fun registerView(viewId: Int, view: MapboxPlatformView) {
        views[viewId] = view
        Log.d(TAG, "View com ID $viewId registrada. Total de views: ${views.size}")
    }

    /**
     * Remove uma instância de MapboxPlatformView quando ela é descartada.
     * @param viewId O ID da view a ser removida.
     */
    fun unregisterView(viewId: Int) {
        views.remove(viewId)
        Log.d(TAG, "View com ID $viewId desregistrada. Total de views: ${views.size}")
    }

    /**
     * Recupera uma instância de MapboxPlatformView pelo seu ID.
     * @param viewId O ID da view a ser recuperada.
     * @return A instância de MapboxPlatformView ou null se não for encontrada.
     */
    fun getView(viewId: Int): MapboxPlatformView? {
        val view = views[viewId]
        if (view == null) {
            Log.w(TAG, "Nenhuma MapboxPlatformView encontrada para o ID: $viewId")
        }
        return view
    }

    /**
     * Retorna todas as views registradas.
     */
    fun getAllViews(): Collection<MapboxPlatformView> {
        return views.values
    }

    /**
     * Limpa todas as views registradas. Deve ser chamado com cautela (ex: em shutdown completo).
     */
    fun clearAllViews() {
        views.clear()
        Log.d(TAG, "Todas as views foram limpas.")
    }
}