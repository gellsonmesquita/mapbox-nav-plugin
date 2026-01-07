package com.plugin.mapboxnav.domain.models


data class NavigationConfig(
    val language: String = "pt-pt",
    val voiceUnits: VoiceUnits = VoiceUnits.IMPERIAL,
    val showManeuvers: Boolean = true,
    val showSpeedLimit: Boolean = true,
    val enableVoiceInstructions: Boolean = true,
    val enableBannerInstructions: Boolean = true,
    val simulateRoute: Boolean = false
)

enum class VoiceUnits {
    IMPERIAL,
    METRIC
}