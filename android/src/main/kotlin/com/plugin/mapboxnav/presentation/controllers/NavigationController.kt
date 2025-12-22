package com.plugin.mapboxnav.presentation.controllers

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.plugin.mapboxnav.core.utils.Logger
import com.plugin.mapboxnav.data.managers.MapboxNavigationManager
import com.plugin.mapboxnav.data.managers.RouteManager
import com.plugin.mapboxnav.data.repository.NavigationRepositoryImpl
import com.plugin.mapboxnav.domain.models.NavigationConfig
import com.plugin.mapboxnav.domain.models.NavigationState
import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.domain.models.RouteInfo
import com.plugin.mapboxnav.domain.models.RouteOptions
import com.plugin.mapboxnav.domain.models.VoiceUnits
import com.plugin.mapboxnav.domain.usecases.CancelNavigationUseCase
import com.plugin.mapboxnav.domain.usecases.ChangeDestinationUseCase
import com.plugin.mapboxnav.domain.usecases.CreateRouteUseCase
import com.plugin.mapboxnav.domain.usecases.StartNavigationUseCase
import com.plugin.mapboxnav.domain.usecases.UpdateNavigationConfigUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NavigationController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Managers
    private val navigationManager = MapboxNavigationManager(context)
    private val routeManager = RouteManager(context)

    // Repository
    private val repository = NavigationRepositoryImpl(context, navigationManager, routeManager)

    // Use Cases
    private val createRouteUseCase = CreateRouteUseCase(repository)
    private val startNavigationUseCase = StartNavigationUseCase(repository)
    private val cancelNavigationUseCase = CancelNavigationUseCase(repository)
    private val changeDestinationUseCase = ChangeDestinationUseCase(repository)
    private val updateConfigUseCase = UpdateNavigationConfigUseCase(repository)

    // Current Configuration
    private var currentConfig = NavigationConfig()

    // State Callback
    private var stateCallback: ((NavigationState) -> Unit)? = null

    fun initialize(lifecycleOwner: LifecycleOwner, config: NavigationConfig = NavigationConfig()) {
        currentConfig = config
        navigationManager.initialize(config)
        MapboxNavigationApp.attach(lifecycleOwner)
        Logger.d("NavigationController initialized")
    }

    fun startFreeDrive() {
        scope.launch {
            try {
                navigationManager.getNavigation()?.startTripSession()
                notifyStateChange(NavigationState.FreeDrive)
                Logger.d("Free drive started")
            } catch (e: Exception) {
                Logger.e("Error starting free drive", e)
                notifyStateChange(NavigationState.Error("Failed to start free drive", e))
            }
        }
    }

    fun createRoute(
        origin: Point,
        destination: Point,
        waypoints: List<Point>? = null,
        onResult: (Result<RouteInfo>) -> Unit
    ) {
        scope.launch {
            val options = RouteOptions(
                origin = origin,
                destination = destination,
                waypoints = waypoints
            )

            val result = createRouteUseCase(options)

            result.onSuccess { routes ->
                if (routes.isNotEmpty()) {
                    val route = routes.first()
                    val routeInfo = RouteInfo(
                        distance = route.directionsRoute.distance() ?: 0.0,
                        duration = route.directionsRoute.duration() ?: 0.0,
                        origin = origin,
                        destination = destination,
                        waypoints = waypoints
                    )
                    onResult(Result.success(routeInfo))
                } else {
                    onResult(Result.failure(Exception("No routes found")))
                }
            }

            result.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun startNavigation(
        origin: Point,
        destination: Point,
        waypoints: List<Point>? = null,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            // First create the route
            val routeOptions = RouteOptions(
                origin = origin,
                destination = destination,
                waypoints = waypoints
            )

            val routeResult = createRouteUseCase(routeOptions)

            routeResult.onSuccess { routes ->
                if (routes.isNotEmpty()) {
                    // Then start navigation with the first route
                    val navResult = startNavigationUseCase(routes.first(), currentConfig)
                    onResult(navResult)
                } else {
                    onResult(Result.failure(Exception("No routes found")))
                }
            }

            routeResult.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun cancelNavigation(onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = cancelNavigationUseCase()
            onResult(result)
        }
    }

    fun changeDestination(newDestination: Point, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = changeDestinationUseCase(newDestination)
            onResult(result)
        }
    }

    fun updateConfig(config: NavigationConfig, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            currentConfig = config
            val result = updateConfigUseCase(config)
            onResult(result)
        }
    }

    fun setManeuverVisibility(visible: Boolean) {
        val newConfig = currentConfig.copy(showManeuvers = visible)
        updateConfig(newConfig) { }
    }

    fun setLanguage(language: String) {
        val newConfig = currentConfig.copy(language = language)
        updateConfig(newConfig) { }
    }

    fun setVoiceUnits(units: VoiceUnits) {
        val newConfig = currentConfig.copy(voiceUnits = units)
        updateConfig(newConfig) { }
    }

    fun enableVoiceInstructions(enabled: Boolean) {
        val newConfig = currentConfig.copy(enableVoiceInstructions = enabled)
        updateConfig(newConfig) { }
    }

    fun getCurrentState(): NavigationState {
        return repository.getCurrentState()
    }

    fun observeNavigationState(callback: (NavigationState) -> Unit) {
        stateCallback = callback
        repository.observeNavigationState(callback)
    }

    private fun notifyStateChange(state: NavigationState) {
        stateCallback?.invoke(state)
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        MapboxNavigationApp.detach(lifecycleOwner)
        Logger.d("NavigationController detached")
    }

    fun cleanup() {
        scope.cancel()
        navigationManager.cleanup()
        stateCallback = null
        Logger.d("NavigationController cleaned up")
    }
}