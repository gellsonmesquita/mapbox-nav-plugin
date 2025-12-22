package com.plugin.mapboxnav.data.managers

import android.content.Context
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.plugin.mapboxnav.core.utils.Logger
import com.plugin.mapboxnav.domain.models.NavigationConfig

class MapboxNavigationManager(private val context: Context) {

    private var mapboxNavigation: MapboxNavigation? = null
    private var currentConfig: NavigationConfig = NavigationConfig()

    fun initialize(config: NavigationConfig = NavigationConfig()) {
        if (!MapboxNavigationApp.isSetup()) {
            val navOptions = NavigationOptions.Builder(context)
                .build()

            MapboxNavigationApp.setup(navOptions)
            Logger.d("MapboxNavigationApp initialized")
        }

        currentConfig = config
        mapboxNavigation = MapboxNavigationApp.current()
    }

    fun getNavigation(): MapboxNavigation? = mapboxNavigation

    fun getCurrentConfig(): NavigationConfig = currentConfig

    fun updateConfig(config: NavigationConfig) {
        currentConfig = config
        applyConfigToNavigation(config)
    }

    private fun applyConfigToNavigation(config: NavigationConfig) {
        mapboxNavigation?.let { nav ->
            if (!config.enableVoiceInstructions) {
                nav.stopTripSession()
            }

            Logger.d("Navigation config updated: $config")
        }
    }

    fun cleanup() {
        mapboxNavigation = null
        Logger.d("MapboxNavigationManager cleaned up")
    }
}