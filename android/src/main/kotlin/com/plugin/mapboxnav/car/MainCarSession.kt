package com.plugin.mapboxnav.car


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.ui.androidauto.map.MapboxCarMapLoader
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.screenmanager.prepareScreens
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.MapboxNavigation


class MainCarSession : Session() {

    // MapboxCarMapLoader handles loading and rendering of the map in Android Auto
    private val carMapLoader = MapboxCarMapLoader()

    // MapboxCarMap provides the map surface for Android Auto
    private val mapboxCarMap = MapboxCarMap().registerObserver(carMapLoader)

    // MapboxCarContext integrates Mapbox Navigation with Android Auto lifecycle
    private val mapboxCarContext = MapboxCarContext(lifecycle, mapboxCarMap)

    // Navigation observer handles starting/stopping trip sessions when attached/detached
    private val navigationObserver = object : MapboxNavigationObserver {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            // Start the trip session to begin receiving location updates
            mapboxNavigation.startTripSession()
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            // Stop the trip session when detached to save battery
            mapboxNavigation.stopTripSession()
        }
    }

    init {
        // Prepare the screen navigation graph for Android Auto
        mapboxCarContext.prepareScreens()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                // Initialize MapboxNavigationApp if not already setup
                if (!MapboxNavigationApp.isSetup()) {
                    MapboxNavigationApp.setup(
                        NavigationOptions.Builder(carContext)
                            .build()
                    )
                }

                // Attach the car lifecycle to MapboxNavigationApp
                // This ensures navigation state is managed with the car session
                MapboxNavigationApp.attach(owner)

                // Register the navigation observer to handle trip session lifecycle
                MapboxNavigationApp.registerObserver(navigationObserver)

                // Setup the MapboxCarMap with SHARED context mode
                // SHARED context is required when using Android Auto widgets
                mapboxCarMap.setup(
                    carContext,
                    MapInitOptions(
                        context = carContext,
                        mapOptions = com.mapbox.maps.MapOptions.Builder()
                            .contextMode(com.mapbox.maps.ContextMode.SHARED)
                            .build()
                    )
                )
            }

            override fun onDestroy(owner: LifecycleOwner) {
                // Clean up: unregister observer and detach from navigation
                MapboxNavigationApp.unregisterObserver(navigationObserver)
                MapboxNavigationApp.detach(owner)
                mapboxCarMap.clearObservers()
            }
        })
    }

    /**
     * Create the initial screen based on location permission status.
     * Returns either the FREE_DRIVE screen or a permission request screen.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateScreen(intent: Intent): Screen {
        // Check if location permission is granted
        val firstScreenKey = if (isLocationPermissionGranted()) {
            // If permission granted, show the Free Drive navigation screen
            MapboxScreenManager.current()?.key ?: MapboxScreen.FREE_DRIVE
        } else {
            // If permission not granted, show permission request screen
            MapboxScreen.NEEDS_LOCATION_PERMISSION
        }

        // Create and return the screen using MapboxScreenManager
        return mapboxCarContext.mapboxScreenManager.createScreen(firstScreenKey)
    }

    /**
     * Handle new intents, including geo deeplinks for navigation.
     * This enables voice-activated navigation ("Navigate to...")
     */
    @OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle geo deeplinks (e.g., geo:latitude,longitude)
        GeoDeeplinkNavigateAction(mapboxCarContext).onNewIntent(intent)
    }

    /**
     * Helper function to check if location permission is granted.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isLocationPermissionGranted(): Boolean {
        return carContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}