package com.plugin.mapboxnav.data.managers

import com.plugin.mapboxnav.core.utils.Logger
import android.content.Context
import com.mapbox.api.directions.v5.models.RouteOptions as MapboxRouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.plugin.mapboxnav.domain.models.RouteOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class RouteManager(private val context: Context) {

    suspend fun requestRoute(
        navigation: MapboxNavigation,
        options: RouteOptions
    ): Result<List<NavigationRoute>> = suspendCancellableCoroutine { continuation ->

        val origin = Point.fromLngLat(options.origin.longitude, options.origin.latitude)
        val destination = Point.fromLngLat(options.destination.longitude, options.destination.latitude)

        val coordinatesList = mutableListOf(origin)
        options.waypoints?.forEach { waypoint ->
            coordinatesList.add(Point.fromLngLat(waypoint.longitude, waypoint.latitude))
        }
        coordinatesList.add(destination)

        val routeOptions = MapboxRouteOptions.builder()
            .applyDefaultNavigationOptions()
            .profile(options.profile.value)
            .coordinatesList(coordinatesList)
            .alternatives(options.alternatives)
            .continueStraight(options.continuesStraight)
            .apply {
                options.exclude?.let { exclude(it.joinToString(",")) }
            }
            .build()

        navigation.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    Logger.d("Routes ready: ${routes.size} routes found")
                    continuation.resume(Result.success(routes))
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: MapboxRouteOptions
                ) {
                    val error = Exception("Route request failed: ${reasons.firstOrNull()?.message}")
                    Logger.e("Route request failed", error)
                    continuation.resume(Result.failure(error))
                }

                override fun onCanceled(routeOptions: MapboxRouteOptions, routerOrigin: String) {
                    val error = Exception("Route request cancelled")
                    Logger.w("Route request cancelled")
                    continuation.resume(Result.failure(error))
                }
            }
        )
    }
}