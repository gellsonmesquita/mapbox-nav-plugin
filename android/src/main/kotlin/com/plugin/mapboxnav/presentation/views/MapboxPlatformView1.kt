package com.plugin.mapboxnav.presentation.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.Location
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.components.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.components.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.components.voice.view.MapboxSoundButton
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.navigation.voice.model.SpeechVolume
import com.plugin.mapboxnav.R
import com.plugin.mapboxnav.data.managers.LocationManager
import com.plugin.mapboxnav.data.managers.MapboxNavigationManager
import com.plugin.mapboxnav.domain.models.Point
import com.plugin.mapboxnav.domain.utils.Logger
import com.plugin.mapboxnav.presentation.lifecycle.LifecycleHelper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.platform.PlatformView
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class MapboxPlatformView1(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private val eventChannelBaseName: String,
    private val viewId: Int,
    private val lifecycleProvider: Lifecycle
) : PlatformView, EventChannel.StreamHandler, DefaultLifecycleObserver {

    private var lifecycleHelper: LifecycleHelper? = null
    private val binding: View by lazy {
        val appCompatThemeId = context.resources.getIdentifier("Theme.AppCompat.NoActionBar", "style", context.packageName)
        val finalThemeId = if (appCompatThemeId != 0) appCompatThemeId else android.R.style.Theme_Material_NoActionBar
        val contextThemeWrapper = ContextThemeWrapper(context, finalThemeId)
        LayoutInflater.from(contextThemeWrapper).inflate(R.layout.maps_navigation, null)
    }
    private val navigationManager = MapboxNavigationManager(context)
    //private val binding: View = LayoutInflater.from(context).inflate(R.layout.maps_navigation, null)
    private val mapView: MapView = binding.findViewById(R.id.mapView)
    private val maneuverView: MapboxManeuverView = binding.findViewById(R.id.maneuverView)
    private val soundButton: MapboxSoundButton = binding.findViewById(R.id.soundButton)
    private val recenter: MapboxRecenterButton = binding.findViewById(R.id.recenter)
    private val routeOverview: MapboxRouteOverviewButton = binding.findViewById(R.id.routeOverview)
    private val navigationLocationProvider = NavigationLocationProvider()


    private var viewportDataSource: MapboxNavigationViewportDataSource? = null
    private var navigationCamera: NavigationCamera? = null
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
    private lateinit var speechApi: MapboxSpeechApi
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            speechApi.clean(value)
        }
    private lateinit var replayProgressObserver: ReplayProgressObserver

    private val eventChannel = EventChannel(messenger, "$eventChannelBaseName/$viewId")
    private var eventSink: EventChannel.EventSink? = null

    private var route: List<NavigationRoute> = emptyList()

    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                soundButton.muteAndExtend(1500L)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                soundButton.unmuteAndExtend(1500L)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    init {
        setupMapAndNavigation()
        setupEventChannel()
        setupNavigation()
        lifecycleProvider.addObserver(this)
    }

    private fun setupMapAndNavigation() {
        val pixelDensity = context.resources.displayMetrics.density
        val routeLineOptions = MapboxRouteLineViewOptions.Builder(context).routeLineBelowLayerId("road-label-navigation").build()
        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(routeLineOptions)
        val originLocation = navigationLocationProvider.lastLocation ?: return
        val originPoint = com.mapbox.geojson.Point.fromLngLat(originLocation.longitude, originLocation.latitude)
        mapView.mapboxMap.loadStyle(NavigationStyles.NAVIGATION_DAY_STYLE) { style ->
            routeLineView.initializeLayers(style)
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .zoom(15.0)
                    .center(originPoint)
                    .build()
            )
            mapView.location.apply {
                setLocationProvider(navigationLocationProvider)
                puckBearingEnabled = true
                enabled = true
                locationPuck = LocationPuck2D(
                    bearingImage = ImageHolder.from(R.drawable.mapbox_user_puck_icon),
                    shadowImage = ImageHolder.from(R.drawable.mapbox_user_icon_shadow),
                )
            }
        }
        viewportDataSource = MapboxNavigationViewportDataSource(mapView.mapboxMap)
        navigationCamera = NavigationCamera(mapView.mapboxMap, mapView.camera, viewportDataSource!!)
        navigationCamera!!.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> recenter.visibility = View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> recenter.visibility = View.VISIBLE
            }
        }
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera!!)
        )
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        recenter.setOnClickListener {
            navigationCamera?.requestNavigationCameraToFollowing()
            routeOverview.showTextAndExtend(1200L)
        }
        routeOverview.setOnClickListener {
            navigationCamera?.requestNavigationCameraToOverview()
            recenter.showTextAndExtend(1200L)
        }
        soundButton.setOnClickListener {
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }
        speechApi = MapboxSpeechApi(
            context,
            Locale.of("pt", "PT").toString()
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            context,
            Locale.of("pt", "PT").toString()
        )
        soundButton.unmute()
    }


    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource?.onRouteProgressChanged(routeProgress)
        viewportDataSource?.evaluate()

        val style = mapView.mapboxMap.style
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    this@MapboxPlatformView1.context,
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
                maneuverView.visibility = View.VISIBLE
                maneuverView.renderManeuvers(maneuvers)
            }
        )
        sendEvent(mapOf(
            "event" to "progress",
            "distanceRemaining" to routeProgress.distanceRemaining,
            "durationRemaining" to routeProgress.durationRemaining,
        ))
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            routeLineApi.setNavigationRoutes(result.navigationRoutes) { value ->
                mapView.mapboxMap.style?.let { routeLineView.renderRouteDrawData(it, value) }
            }
            navigationCamera?.requestNavigationCameraToFollowing()
            viewportDataSource?.onRouteChanged(result.navigationRoutes.first())
            viewportDataSource?.evaluate()
        } else {
            mapView.mapboxMap.style?.let {
                routeLineApi.clearRouteLine { value -> routeLineView.renderClearRouteLineValue(it, value) }
                routeArrowView.render(it, routeArrowApi.clearArrows())
            }
            viewportDataSource?.clearRouteData()
            viewportDataSource?.evaluate()
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }


    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun setupNavigation() {
        navigationManager.initialize()
        val nav = navigationManager.getNavigation()
        nav?.registerLocationObserver(locationObserver)
        nav?.registerRouteProgressObserver(routeProgressObserver)
        nav?.registerRoutesObserver(routesObserver)
        nav?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        nav?.mapboxReplayer?.let { replayProgressObserver = ReplayProgressObserver(it) }
        nav?.registerRouteProgressObserver(replayProgressObserver)

        nav?.startTripSession()
    }

    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }


    override fun onFlutterViewAttached(flutterView: View) {
        val context = flutterView.context
        val shouldDestroyOnDestroy = when (context is FlutterActivity) {
            true -> context.shouldDestroyEngineWithHost()
            false -> true
        }
        lifecycleHelper = LifecycleHelper(lifecycleProvider, shouldDestroyOnDestroy)
        mapView.setViewTreeLifecycleOwner(lifecycleHelper)
    }

    private fun setupEventChannel() {
        eventChannel.setStreamHandler(this)
    }

    private fun handleNavigationStateChange(state: NavigationSessionState) {
        when (state) {
            is NavigationSessionState.Idle -> {
                sendEvent(mapOf("event" to "idle"))
            }
            is NavigationSessionState.FreeDrive -> {
                sendEvent(mapOf("event" to "freeDrive"))
            }
            is NavigationSessionState.ActiveGuidance -> {
                sendEvent(mapOf(
                    "event" to "routeCreated",
                ))
            }
        }
    }

    fun createRoute(
        origin: List<Double>,
        destination: List<Double>,
        waypoints: List<List<Double>>? = null
    ) {
        val destPoint = com.mapbox.geojson.Point.fromLngLat(destination[0], destination[1])
        val waypointsList = waypoints?.map { point ->
            Point(point[0], point[1])
        }
        findRoute(destPoint)
    }

    private fun findRoute(destination: com.mapbox.geojson.Point) {
        val originLocation = navigationLocationProvider.lastLocation ?: return
        val originPoint = com.mapbox.geojson.Point.fromLngLat(originLocation.longitude, originLocation.latitude)

        val nav =  navigationManager.getNavigation() ?: return
        nav.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(originPoint, destination))
                .apply {
                    originLocation.bearing?.let { bearing ->
                        bearingsList(
                            listOf(
                                Bearing.builder()
                                    .angle(bearing)
                                    .degrees(45.0)
                                    .build(),
                                null
                            )
                        )
                    }
                }
                .layersList(listOf(nav.getZLevel(), null))
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    route = routes
                    sendEvent(
                        mapOf(
                            "event" to "routeCreated",
                            "routes" to routes.map { route ->
                                mapOf(
                                    "distance" to route.directionsRoute.distance(),
                                    "duration" to route.directionsRoute.duration()
                                )
                            }
                        )

                    )
                }
            }
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun performStart(origin: Point?, dest: Point?, waypoints: List<Point>?) {
        val nav = navigationManager.getNavigation() ?: return
        nav.setNavigationRoutes(route)
        soundButton.visibility = View.VISIBLE
        routeOverview.visibility = View.VISIBLE
        navigationCamera?.requestNavigationCameraToOverview()
        nav.startTripSession()
    }



    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun cancelNavigation() {
        val nav = navigationManager.getNavigation() ?: return
        nav.setNavigationRoutes(emptyList())
        soundButton.visibility = View.INVISIBLE
        maneuverView.visibility = View.INVISIBLE
        routeOverview.visibility = View.INVISIBLE
        nav.stopTripSession()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun finishNavigation() {
        cancelNavigation()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun stopNavigation() {
        cancelNavigation()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun changeDestination(newDestination: List<Double>) {
        if (newDestination.size < 2) {
            Logger.e("Failed to change destination: Invalid coordinates")
            return
        }
        val destPoint = com.mapbox.geojson.Point.fromLngLat(newDestination[0], newDestination[1])
        findRoute(destPoint)
        performStart(null, null, null);
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

    override fun getView(): View = binding

    fun getNavigation(): MapboxNavigation? = navigationManager.getNavigation()

    @SuppressLint("Lifecycle")
    override fun dispose() {
        lifecycleProvider.removeObserver(this)
        eventChannel.setStreamHandler(null)
        getNavigation()?.apply {
            unregisterLocationObserver(locationObserver)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterRoutesObserver(routesObserver)
            unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            unregisterRouteProgressObserver(replayProgressObserver)
        }
        mapView.onDestroy()

        Logger.d("MapboxPlatformView disposed: $viewId")
    }

    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {}

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            viewportDataSource?.onLocationChanged(enhancedLocation)
            viewportDataSource?.evaluate()

            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera?.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0)
                        .build()
                )
            }
        }
    }

}