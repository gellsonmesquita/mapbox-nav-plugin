package com.plugin.mapboxnav.domain.usecases

import com.mapbox.navigation.base.route.NavigationRoute
import com.plugin.mapboxnav.domain.models.RouteOptions
import com.plugin.mapboxnav.domain.repository.NavigationRepository

class CreateRouteUseCase(
    private val repository: NavigationRepository
) {
    suspend operator fun invoke(options: RouteOptions): Result<List<NavigationRoute>> {
        return try {
            repository.createRoute(options)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}