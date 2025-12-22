package com.plugin.mapboxnav.domain.usecases

import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.domain.repository.NavigationRepository


class ChangeDestinationUseCase(
    private val repository: NavigationRepository
) {
    suspend operator fun invoke(newDestination: Point): Result<Unit> {
        return try {
            repository.changeDestination(newDestination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}