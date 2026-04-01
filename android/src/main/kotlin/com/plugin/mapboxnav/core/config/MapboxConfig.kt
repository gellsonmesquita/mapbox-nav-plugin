package com.plugin.mapboxnav.core.config

object MapboxConfig {
    var accessToken: String? = null
    const val VIEW_TYPE_ID = "mapView"
    const val METHOD_CHANNEL_NAME = "nav_channel"
    const val EVENT_CHANNEL_BASE_NAME = "nav_event"
    // Android Auto Configuration
    const val CAR_CONTEXT_MODE = "SHARED"
    const val DEFAULT_ZOOM_LEVEL = 15.0
    // Navigation Configuration
    const val DEFAULT_LANGUAGE = "en"
    const val DEFAULT_VOICE_UNITS = "imperial"

    // Performance Policy (0 keeps legacy behavior)
    const val DEFAULT_ROUTE_REQUEST_COOLDOWN_MS = 0L
    const val DEFAULT_LOCATION_EVENT_INTERVAL_MS = 0L
    const val DEFAULT_TRIP_PROGRESS_EVENT_INTERVAL_MS = 0L
    const val DEFAULT_OFFLINE_PROGRESS_EVENT_INTERVAL_MS = 0L
    const val DEFAULT_SKIP_DUPLICATE_ROUTE_REQUESTS = false
}