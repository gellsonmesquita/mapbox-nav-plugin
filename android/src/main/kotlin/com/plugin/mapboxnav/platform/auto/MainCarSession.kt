package com.plugin.mapboxnav.platform.auto


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.ContextMode
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.navigation.ui.androidauto.map.MapboxCarMapLoader
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.screenmanager.prepareScreens
import com.plugin.mapboxnav.core.utils.Logger

class MainCarSession : Session() {

    private val carMapLoader = MapboxCarMapLoader()

    private val mapboxCarMap = MapboxCarMap().registerObserver(carMapLoader)

    private val mapboxCarContext = MapboxCarContext(lifecycle, mapboxCarMap)

    private lateinit var carNavigationBridge: CarNavigationBridge

    private val navigationObserver = object : MapboxNavigationObserver {
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            if (hasLocationPermission()) {
                mapboxNavigation.startTripSession()
                Logger.d("Trip session started in car")
            } else {
                Logger.w("Location permission not granted, cannot start trip session")
            }
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.stopTripSession()
            Logger.d("Trip session stopped in car")
        }
    }

    init {
        carNavigationBridge = CarNavigationBridge.getInstance(carContext)

        mapboxCarContext.prepareScreens()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                Logger.d("Car session onCreate")

                if (!MapboxNavigationApp.isSetup()) {
                    MapboxNavigationApp.setup(
                        NavigationOptions.Builder(carContext)
                            .build()
                    )
                    Logger.d("MapboxNavigationApp initialized in car")
                }

                carNavigationBridge.initialize(owner)
                carNavigationBridge.onCarSessionStarted()

                MapboxNavigationApp.attach(owner)

                MapboxNavigationApp.registerObserver(navigationObserver)

                mapboxCarMap.setup(
                    carContext,
                    MapInitOptions(
                        context = carContext,
                        mapOptions = MapOptions.Builder()
                            .contextMode(ContextMode.SHARED)
                            .build()
                    )
                )

                Logger.d("Car map setup completed")
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Logger.d("Car session onDestroy")

                MapboxNavigationApp.unregisterObserver(navigationObserver)
                MapboxNavigationApp.detach(owner)
                mapboxCarMap.clearObservers()

                carNavigationBridge.onCarSessionEnded()

                Logger.d("Car session cleaned up")
            }
        })
    }


    override fun onCreateScreen(intent: Intent): Screen {
        Logger.d("Creating car screen")

        val firstScreenKey = if (isLocationPermissionGranted()) {
            MapboxScreenManager.current()?.key ?: MapboxScreen.FREE_DRIVE
        } else {
            MapboxScreen.NEEDS_LOCATION_PERMISSION
        }

        return mapboxCarContext.mapboxScreenManager.createScreen(firstScreenKey)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("Car session received new intent: ${intent.action}")

        GeoDeeplinkNavigateAction(mapboxCarContext).onNewIntent(intent)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isLocationPermissionGranted()
        } else {
            true // Permissions granted at install time on older versions
        }
    }
}