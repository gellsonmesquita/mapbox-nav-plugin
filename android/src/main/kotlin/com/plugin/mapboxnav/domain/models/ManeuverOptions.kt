package com.plugin.mapboxnav.domain.models

data class ManeuverOptions(
    val visible: Boolean = true,
    val primaryTextSize: Float = 16f,
    val secondaryTextSize: Float = 14f,
    val showStepDistance: Boolean = true,
    val showArrivalTime: Boolean = true
)