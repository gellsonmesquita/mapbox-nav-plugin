package com.plugin.mapboxnav.domain.utils

import com.plugin.mapboxnav.domain.models.PerformancePolicy

class RouteRequestGate(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    enum class SkipReason {
        IN_FLIGHT_DUPLICATE,
        COOLDOWN_DUPLICATE,
    }

    private var inFlightSignature: String? = null
    private var lastRequestSignature: String? = null
    private var lastRequestAtMs: Long = 0

    fun shouldSkip(signature: String, policy: PerformancePolicy): Boolean {
        return skipReason(signature, policy) != null
    }

    fun skipReason(signature: String, policy: PerformancePolicy): SkipReason? {
        val nowMs = nowProvider()
        if (policy.skipDuplicateRouteRequests && inFlightSignature == signature) {
            return SkipReason.IN_FLIGHT_DUPLICATE
        }

        if (policy.routeRequestCooldownMs > 0 &&
            signature == lastRequestSignature &&
            nowMs - lastRequestAtMs < policy.routeRequestCooldownMs
        ) {
            return SkipReason.COOLDOWN_DUPLICATE
        }

        return null
    }

    fun markInFlight(signature: String) {
        inFlightSignature = signature
        lastRequestSignature = signature
        lastRequestAtMs = nowProvider()
    }

    fun clearInFlight() {
        inFlightSignature = null
    }

    fun reset() {
        inFlightSignature = null
        lastRequestSignature = null
        lastRequestAtMs = 0
    }
}

