package com.plugin.mapboxnav.domain.usecases

import com.mapbox.navigation.base.route.NavigationRoute
import com.plugin.mapboxnav.domain.models.NavigationConfig
import com.plugin.mapboxnav.domain.repository.NavigationRepository


class StartNavigationUseCase(
    private val repository: NavigationRepository
) {
    suspend operator fun invoke(
        route: NavigationRoute,
        config: NavigationConfig = NavigationConfig()
    ): Result<Unit> {
        return try {
            repository.startNavigation(route, config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}