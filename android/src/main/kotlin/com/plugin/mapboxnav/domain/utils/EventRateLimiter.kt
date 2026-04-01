package com.plugin.mapboxnav.domain.utils

class EventRateLimiter {
    private var lastEmissionMs: Long = 0

    fun shouldEmit(minIntervalMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (minIntervalMs <= 0) {
            lastEmissionMs = nowMs
            return true
        }
        if (nowMs - lastEmissionMs < minIntervalMs) {
            return false
        }
        lastEmissionMs = nowMs
        return true
    }

    fun reset() {
        lastEmissionMs = 0
    }
}

