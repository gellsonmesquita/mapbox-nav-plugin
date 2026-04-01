package com.plugin.mapboxnav.domain.models

import com.plugin.mapboxnav.core.config.MapboxConfig

data class PerformancePolicy(
    val routeRequestCooldownMs: Long = MapboxConfig.DEFAULT_ROUTE_REQUEST_COOLDOWN_MS,
    val locationEventMinIntervalMs: Long = MapboxConfig.DEFAULT_LOCATION_EVENT_INTERVAL_MS,
    val tripProgressEventMinIntervalMs: Long = MapboxConfig.DEFAULT_TRIP_PROGRESS_EVENT_INTERVAL_MS,
    val offlineProgressEventMinIntervalMs: Long = MapboxConfig.DEFAULT_OFFLINE_PROGRESS_EVENT_INTERVAL_MS,
    val skipDuplicateRouteRequests: Boolean = MapboxConfig.DEFAULT_SKIP_DUPLICATE_ROUTE_REQUESTS,
) {
    companion object {
        fun fromMap(data: Map<String, Any>?): PerformancePolicy {
            if (data == null) return PerformancePolicy()
            return PerformancePolicy(
                routeRequestCooldownMs = (data["routeRequestCooldownMs"] as? Number)?.toLong()
                    ?: MapboxConfig.DEFAULT_ROUTE_REQUEST_COOLDOWN_MS,
                locationEventMinIntervalMs = (data["locationEventMinIntervalMs"] as? Number)?.toLong()
                    ?: MapboxConfig.DEFAULT_LOCATION_EVENT_INTERVAL_MS,
                tripProgressEventMinIntervalMs = (data["tripProgressEventMinIntervalMs"] as? Number)?.toLong()
                    ?: MapboxConfig.DEFAULT_TRIP_PROGRESS_EVENT_INTERVAL_MS,
                offlineProgressEventMinIntervalMs = (data["offlineProgressEventMinIntervalMs"] as? Number)?.toLong()
                    ?: MapboxConfig.DEFAULT_OFFLINE_PROGRESS_EVENT_INTERVAL_MS,
                skipDuplicateRouteRequests = data["skipDuplicateRouteRequests"] as? Boolean
                    ?: MapboxConfig.DEFAULT_SKIP_DUPLICATE_ROUTE_REQUESTS,
            )
        }
    }
}

