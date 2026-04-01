package com.plugin.mapboxnav.domain.models

data class NavigationBehaviorPolicy(
    val autoOverviewOnRouteReady: Boolean = true,
    val autoFollowOnFirstLocation: Boolean = true,
    val autoFollowOnDestinationChange: Boolean = true,
    val autoStartTripSession: Boolean = true,
) {
    companion object {
        fun fromMap(data: Map<String, Any>?): NavigationBehaviorPolicy {
            if (data == null) return NavigationBehaviorPolicy()
            return NavigationBehaviorPolicy(
                autoOverviewOnRouteReady = data["autoOverviewOnRouteReady"] as? Boolean ?: true,
                autoFollowOnFirstLocation = data["autoFollowOnFirstLocation"] as? Boolean ?: true,
                autoFollowOnDestinationChange = data["autoFollowOnDestinationChange"] as? Boolean ?: true,
                autoStartTripSession = data["autoStartTripSession"] as? Boolean ?: true,
            )
        }
    }
}

