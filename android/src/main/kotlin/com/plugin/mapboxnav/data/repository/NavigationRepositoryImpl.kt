package com.plugin.mapboxnav.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.plugin.mapboxnav.data.managers.MapboxNavigationManager
import com.plugin.mapboxnav.data.managers.RouteManager
import com.plugin.mapboxnav.domain.models.NavigationConfig
import com.plugin.mapboxnav.domain.models.NavigationState
import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.domain.models.RouteInfo
import com.plugin.mapboxnav.domain.repository.NavigationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.plugin.mapboxnav.domain.models.RouteOptions
import com.plugin.mapboxnav.domain.utils.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class NavigationRepositoryImpl(
    private val context: Context,
    private val navigationManager: MapboxNavigationManager,
    private val routeManager: RouteManager
) : NavigationRepository {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    private val navigationState: StateFlow<NavigationState> = _navigationState

    private var currentRoute: NavigationRoute? = null
    private var currentRouteInfo: RouteInfo? = null


    override suspend fun createRoute(options: RouteOptions): Result<List<NavigationRoute>> =
        suspendCoroutine { continuation ->
            var navigation = navigationManager.getNavigation()
            if (navigation == null) {
                continuation.resume(Result.failure(Exception("MapboxNavigation not initialized")))
                return@suspendCoroutine
            }
            val origin = com.mapbox.geojson.Point.fromLngLat(
                options.origin.longitude,
                options.origin.latitude
            )
            val destination = com.mapbox.geojson.Point.fromLngLat(
                options.destination.longitude,
                options.destination.latitude
            )
            val mapboxOptions = com.mapbox.api.directions.v5.models.RouteOptions.builder()
                .coordinatesList(listOf(origin,destination))
                .profile(options.profile.value)
                .alternatives(options.alternatives)
                .build()


            navigation.requestRoutes(mapboxOptions, object : NavigationRouterCallback {
                override fun onCanceled(
                    routeOptions: com.mapbox.api.directions.v5.models.RouteOptions,
                    routerOrigin: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: com.mapbox.api.directions.v5.models.RouteOptions
                ) {

                }

                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    if (routes.isNotEmpty()) {
                        val firstRoute = routes.first()
                        currentRoute = firstRoute
                        currentRouteInfo = RouteInfo(
                            distance = firstRoute.directionsRoute.distance(),
                            duration = firstRoute.directionsRoute.duration(),
                            origin = options.origin,
                            destination = options.destination,
                            waypoints = options.waypoints
                        )
                        _navigationState.value = NavigationState.RouteCreated(currentRouteInfo!!)
                        continuation.resume(Result.success(routes))
                    } else {
                        continuation.resume(Result.failure(Exception("No routes found")))
                    }
                }
            })
        }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun startNavigation(
        route: NavigationRoute,
        config: NavigationConfig
    ): Result<Unit> {
        val navigation = navigationManager.getNavigation() ?: return Result.failure(Exception("MapboxNavigation not initialized"))
        return try {
            navigationManager.updateConfig(config)
            navigation.setNavigationRoutes(listOf(route))
            navigation.startTripSession()
            currentRoute = route
            currentRouteInfo?.let {
                _navigationState.value = NavigationState.NavigationActive(it)
            }
            Logger.d("Navigation started successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error starting navigation", e)
            _navigationState.value = NavigationState.Error("Failed to start navigation", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelNavigation(): Result<Unit> {
        val navigation = navigationManager.getNavigation()
            ?: return Result.failure(Exception("MapboxNavigation not initialized"))

        return try {
            navigation.setNavigationRoutes(emptyList())
            navigation.stopTripSession()

            currentRoute = null
            currentRouteInfo = null
            _navigationState.value = NavigationState.NavigationCancelled("User cancelled")

            Logger.d("Navigation cancelled")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error cancelling navigation", e)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun changeDestination(newDestination: Point): Result<Unit> {
        val currentOrigin = currentRouteInfo?.origin
            ?: return Result.failure(Exception("No active route to change"))

        val routeOptions = RouteOptions(
            origin = currentOrigin,
            destination = newDestination
        )

        return createRoute(routeOptions).mapCatching { routes ->
            if (routes.isNotEmpty()) {
                startNavigation(routes.first(), navigationManager.getCurrentConfig())
                Logger.d("Destination changed successfully")
            }
        }
    }

    override suspend fun updateNavigationConfig(config: NavigationConfig): Result<Unit> {
        return try {
            navigationManager.updateConfig(config)
            Logger.d("Navigation config updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("Error updating config", e)
            Result.failure(e)
        }
    }

    override fun getCurrentState(): NavigationState {
        return navigationState.value
    }

    override fun observeNavigationState(callback: (NavigationState) -> Unit) {
        // This would typically use Flow collection
        // For simplicity, we'll just call the callback with current state
        callback(navigationState.value)
    }
}