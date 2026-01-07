package com.plugin.mapboxnav.domain.models


data class RouteOptions(
    val origin: Point,
    val destination: Point,
    val waypoints: List<Point>? = null,
    val profile: RouteProfile = RouteProfile.DRIVING_TRAFFIC,
    val alternatives: Boolean = true,
    val continuesStraight: Boolean = true,
    val exclude: List<String>? = null
)

data class Point(
    val latitude: Double,
    val longitude: Double
)