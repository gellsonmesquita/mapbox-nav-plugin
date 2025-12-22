package com.plugin.mapboxnav.domain.models


sealed class NavigationState {
    object Idle : NavigationState()
    object FreeDrive : NavigationState()
    data class RouteCreated(val routeInfo: RouteInfo) : NavigationState()
    data class NavigationActive(val routeInfo: RouteInfo) : NavigationState()
    data class NavigationCompleted(val destination: Point) : NavigationState()
    data class NavigationCancelled(val reason: String?) : NavigationState()
    data class Error(val message: String, val throwable: Throwable? = null) : NavigationState()
}

data class RouteInfo(
    val distance: Double,
    val duration: Double,
    val origin: Point,
    val destination: Point,
    val waypoints: List<Point>? = null
)

data class Point(val latitude: Double, val longitude: Double)