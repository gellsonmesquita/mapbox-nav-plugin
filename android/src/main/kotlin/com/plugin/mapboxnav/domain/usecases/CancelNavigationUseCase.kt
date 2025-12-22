package com.plugin.mapboxnav.domain.usecases

import com.plugin.mapboxnav.domain.repository.NavigationRepository


class CancelNavigationUseCase(
    private val repository: NavigationRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            repository.cancelNavigation()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}