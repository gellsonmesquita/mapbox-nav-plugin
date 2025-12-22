package com.plugin.mapboxnav.domain.usecases

import com.plugin.mapboxnav.domain.models.NavigationConfig
import com.plugin.mapboxnav.domain.repository.NavigationRepository


class UpdateNavigationConfigUseCase(
    private val repository: NavigationRepository
) {
    suspend operator fun invoke(config: NavigationConfig): Result<Unit> {
        return try {
            repository.updateNavigationConfig(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}