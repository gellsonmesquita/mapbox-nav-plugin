package com.plugin.mapboxnav.presentation.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.plugin.mapboxnav.core.utils.Logger
import com.plugin.mapboxnav.data.managers.LocationManager
import com.plugin.mapboxnav.domain.models.NavigationState
import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.presentation.controllers.NavigationController
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.platform.PlatformView

class MapboxPlatformView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private val eventChannelBaseName: String,
    private val viewId: Int,
    private val lifecycleProvider: Lifecycle
) : PlatformView, EventChannel.StreamHandler, DefaultLifecycleObserver {

    private val container: FrameLayout = FrameLayout(context)
    var mapView: MapView? = null
        private set

    private val eventChannel: EventChannel = EventChannel(
        messenger,
        "$eventChannelBaseName/$viewId"
    )

    private var eventSink: EventChannel.EventSink? = null
    private val navigationController = NavigationController(context)

    init {
        setupMapView()
        setupEventChannel()
        setupNavigationController()
        lifecycleProvider.addObserver(this)
    }

    private fun setupMapView() {
        try {
            val mapInitOptions = MapInitOptions(
                context = context,
                mapOptions = com.mapbox.maps.MapOptions.Builder()
                    .contextMode(com.mapbox.maps.ContextMode.SHARED)
                    .build()
            )

            mapView = MapView(context, mapInitOptions).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
            }

            container.addView(mapView)
            Logger.d("MapView created for viewId: $viewId")
        } catch (e: Exception) {
            Logger.e("Error creating MapView", e)
            sendEvent(mapOf("error" to "Failed to create map: ${e.message}"))
        }
    }

    private fun setupEventChannel() {
        eventChannel.setStreamHandler(this)
    }

    private fun setupNavigationController() {
        lifecycleProvider.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                navigationController.initialize(owner)
                navigationController.observeNavigationState { state ->
                    handleNavigationStateChange(state)
                }
            }
        })
    }

    private fun handleNavigationStateChange(state: NavigationState) {
        when (state) {
            is NavigationState.Idle -> {
                sendEvent(mapOf("event" to "idle"))
            }
            is NavigationState.FreeDrive -> {
                sendEvent(mapOf("event" to "freeDrive"))
            }
            is NavigationState.RouteCreated -> {
                sendEvent(mapOf(
                    "event" to "routeCreated",
                    "distance" to state.routeInfo.distance,
                    "duration" to state.routeInfo.duration
                ))
            }
            is NavigationState.NavigationActive -> {
                sendEvent(mapOf(
                    "event" to "navigationActive",
                    "distance" to state.routeInfo.distance,
                    "duration" to state.routeInfo.duration
                ))
            }
            is NavigationState.NavigationCompleted -> {
                sendEvent(mapOf(
                    "event" to "navigationCompleted",
                    "latitude" to state.destination.latitude,
                    "longitude" to state.destination.longitude
                ))
            }
            is NavigationState.NavigationCancelled -> {
                sendEvent(mapOf(
                    "event" to "navigationCancelled",
                    "reason" to state.reason
                ))
            }
            is NavigationState.Error -> {
                sendEvent(mapOf(
                    "event" to "error",
                    "message" to state.message
                ))
            }
        }
    }

    fun createRoute(
        origin: List<Double>,
        destination: List<Double>,
        waypoints: List<List<Double>>? = null
    ) {
        val originPoint = Point(origin[0], origin[1])
        val destPoint = Point(destination[0], destination[1])

        val waypointsList = waypoints?.map { point ->
            Point(point[0], point[1])
        }

        navigationController.createRoute(originPoint, destPoint, waypointsList) { result ->
            result.onSuccess { routeInfo ->
                Logger.d("Route created: ${routeInfo.distance}m, ${routeInfo.duration}s")
            }.onFailure { error ->
                Logger.e("Failed to create route", error)
            }
        }
    }

    fun startNavigation(
        origin: List<Double>?,
        destination: List<Double>,
        waypoints: List<List<Double>>? = null
    ) {
        val destPoint = Point(destination[0], destination[1])
        val waypointsList = waypoints?.map { Point(it[0], it[1]) }

        if (origin != null && origin.size >= 2) {
            val originPoint = Point(origin[0], origin[1])
            performStart(originPoint, destPoint, waypointsList)
        } else {
            val locationManager = LocationManager(context)
            locationManager.getCurrentLocation { currentPoint ->
                val finalOrigin = currentPoint ?: Point(0.0, 0.0)
                performStart(finalOrigin, destPoint, waypointsList)
            }
        }
    }

    private fun performStart(origin: Point, dest: Point, waypoints: List<Point>?) {
        navigationController.startNavigation(origin, dest, waypoints) { result ->
            result.onSuccess { Logger.d("Navigation started") }
            result.onFailure { Logger.e("Failed to start", it) }
        }
    }

    fun cancelNavigation() {
        navigationController.cancelNavigation { result ->
            result.onSuccess {
                Logger.d("Navigation cancelled")
            }.onFailure { error ->
                Logger.e("Failed to cancel navigation", error)
            }
        }
    }

    fun finishNavigation() {
        cancelNavigation()
    }

    fun stopNavigation() {
        cancelNavigation()
    }

    fun changeDestination(newDestination: List<Double>) {
        if (newDestination.size < 2) {
            Logger.e("Failed to change destination: Invalid coordinates")
            return
        }
        val destPoint = Point(newDestination[0], newDestination[1])
        navigationController.changeDestination(destPoint) { result ->
            result.onSuccess {
                Logger.d("Destination changed successfully")
            }.onFailure { error ->
                Logger.e("Failed to change destination", error)
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
        Logger.d("Event channel listener attached for viewId: $viewId")
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        Logger.d("Event channel listener cancelled for viewId: $viewId")
    }

    private fun sendEvent(data: Map<String, Any?>) {
        eventSink?.success(data)
    }

    // Lifecycle Methods
    override fun onCreate(owner: LifecycleOwner) {
        Logger.d("MapboxPlatformView onCreate: $viewId")
    }

    override fun onStart(owner: LifecycleOwner) {
        mapView?.onStart()
    }

    override fun onResume(owner: LifecycleOwner) {
        // Not typically needed for MapView
    }

    override fun onPause(owner: LifecycleOwner) {
        // Not typically needed for MapView
    }

    override fun onStop(owner: LifecycleOwner) {
        mapView?.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        dispose()
    }

    override fun getView(): View = container

    override fun dispose() {
        lifecycleProvider.removeObserver(this)
        eventChannel.setStreamHandler(null)
        mapView?.onDestroy()
        mapView = null
        navigationController.cleanup()
        Logger.d("MapboxPlatformView disposed: $viewId")
    }
}