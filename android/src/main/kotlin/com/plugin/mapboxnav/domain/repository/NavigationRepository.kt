package com.plugin.mapboxnav.domain.repository

import com.mapbox.navigation.base.route.NavigationRoute

import com.plugin.mapboxnav.domain.models.NavigationConfig
import com.plugin.mapboxnav.domain.models.NavigationState
import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.domain.models.RouteOptions


interface NavigationRepository {
    suspend fun createRoute(options: RouteOptions): Result<List<NavigationRoute>>
    suspend fun startNavigation(route: NavigationRoute, config: NavigationConfig): Result<Unit>
    suspend fun cancelNavigation(): Result<Unit>
    suspend fun changeDestination(newDestination: Point): Result<Unit>
    suspend fun updateNavigationConfig(config: NavigationConfig): Result<Unit>
    fun getCurrentState(): NavigationState
    fun observeNavigationState(callback: (NavigationState) -> Unit)
}