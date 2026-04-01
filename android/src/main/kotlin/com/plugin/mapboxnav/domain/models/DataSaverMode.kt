package com.plugin.mapboxnav.domain.models

enum class DataSaverMode {
    OFF,
    BALANCED,
    AGGRESSIVE;

    companion object {
        fun from(raw: String?): DataSaverMode {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OFF
        }
    }
}

