package com.plugin.mapboxnav.presentation.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
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
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Polygon
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxPlatformView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private val eventChannelBaseName: String,
    private val viewId: Int,
    private var activityPluginBinding: ActivityPluginBinding?,
    private val lifecycleProvider: Lifecycle,
) : PlatformView, EventChannel.StreamHandler, DefaultLifecycleObserver {

    private val TAG = "MapboxPlatformView"
    private var eventChannel: EventChannel
    private var eventSink: EventSink? = null
    private var methodChannel: MethodChannel

    private lateinit var containerView: View
    private var _mapView: MapView? = null
    val mapView: MapView? get() = _mapView

    private var mapboxNavigation: MapboxNavigation? = null
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private var navigationCamera: NavigationCamera? = null
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeArrowApi: MapboxRouteArrowApi
    private lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var speechApi: MapboxSpeechApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
    private val navigationLocationProvider = NavigationLocationProvider()
    private var currentDirectionsRoute: DirectionsRoute? = null
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            voiceInstructionsPlayer.volume(SpeechVolume(if (value) 0f else 1f))
            sendEvent("voiceInstructionsMuted", mapOf("muted" to value))
        }
    private var lifecycleHelper: LifecycleHelper? = null

    private val tileStore: TileStore by lazy { TileStore.create() }
    private val offlineManager: OfflineManager by lazy { OfflineManager() }

    private val pixelDensity = context.resources.displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            val primaryRoute = routeUpdateResult.navigationRoutes.first()
            currentDirectionsRoute = primaryRoute.directionsRoute
            Log.d(TAG, "Rota atualizada. Distância da rota: ${primaryRoute.directionsRoute.distance()}")
            cacheRouteData(primaryRoute)
            routeLineApi.setNavigationRoutes(routeUpdateResult.navigationRoutes) { value ->
                _mapView?.mapboxMap?.style?.apply { routeLineView.renderRouteDrawData(this, value) }
            }
            viewportDataSource.onRouteChanged(primaryRoute)
            viewportDataSource.evaluate()
            sendEvent("routeCreated", mapOf(
                "routeId" to primaryRoute.directionsRoute.hashCode().toString(),
                "routeCount" to routeUpdateResult.navigationRoutes.size,
                "distance" to currentDirectionsRoute?.distance(),
                "duration" to currentDirectionsRoute?.duration()
            ))
        } else {
            routeLineApi.clearRouteLine { value ->
                _mapView?.mapboxMap?.style?.apply { routeLineView.renderClearRouteLineValue(this, value) }
            }
            mapView?.mapboxMap?.style?.let {
                routeLineApi.clearRouteLine { value -> routeLineView.renderClearRouteLineValue(it, value) }
                routeArrowView.render(it, routeArrowApi.clearArrows())
            }
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
            currentDirectionsRoute = null
            sendEvent("routeCreated", mapOf(
                "routeCount" to 0,
                "distance" to 0,
                "duration" to 0
            ))
            Log.d(TAG, "Nenhuma rota encontrada no RouteObserver.")
        }
    }

    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {}

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val mapView = _mapView ?: return
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )
            sendEvent("locationUpdate", mapOf(
                "latitude" to enhancedLocation.latitude,
                "longitude" to enhancedLocation.longitude,
                "bearing" to enhancedLocation.bearing,
                "accuracy" to enhancedLocation.bearingAccuracy,
                "speed" to enhancedLocation.speed
            ))
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera?.requestNavigationCameraToFollowing()
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        routeLineApi.updateWithRouteProgress(routeProgress) { value ->
            _mapView?.mapboxMap?.style?.apply {
                routeLineView.renderRouteLineUpdate(this, value)
            }
        }

        val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        _mapView?.mapboxMap?.style?.apply { routeArrowView.renderManeuverUpdate(this, maneuverArrowResult) }

        maneuverApi.getManeuvers(routeProgress).fold(
            { error ->
                sendEvent("maneuverError", mapOf("message" to error.errorMessage))
            },
            { maneuvers ->
                val maneuverList = maneuvers.map { maneuver ->
                    mapOf(
                        "instruction" to (maneuver.primary.text ?: ""),
                        "distance" to (maneuver.stepDistance ?: 0.0)
                    )
                }
                sendEvent("maneuverUpdate", mapOf("maneuvers" to maneuverList))
            }
        )

        val tripProgress = tripProgressApi.getTripProgress(routeProgress)
        sendEvent("tripProgressUpdate", mapOf(
            "distanceRemaining" to tripProgress.distanceRemaining,
            "timeRemaining" to tripProgress.totalTimeRemaining,
            "percentRouteTraveled" to tripProgress.percentRouteTraveled,
            "estimatedTimeToArrival" to tripProgress.estimatedTimeToArrival
        ))
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    private val speechCallback = MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
        expected.fold(
            { error ->
                voiceInstructionsPlayer.play(error.fallback, voiceInstructionsPlayerCallback)
            },
            { value ->
                voiceInstructionsPlayer.play(value.announcement, voiceInstructionsPlayerCallback)
            }
        )
    }

    private val voiceInstructionsPlayerCallback = MapboxNavigationConsumer<SpeechAnnouncement> { value ->
        speechApi.clean(value)
    }

    init {
        eventChannel = EventChannel(messenger, "$eventChannelBaseName/$viewId")
        eventChannel.setStreamHandler(this)
        methodChannel = MethodChannel(messenger, "$eventChannelBaseName/$viewId/methods")
        val fiveGigabytes = 5L * 1024 * 1024 * 1024
        tileStore.setOption(TileStoreOptions.DISK_QUOTA, Value(fiveGigabytes))
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "toggleVoiceInstructions" -> {
                    isVoiceInstructionsMuted = !isVoiceInstructionsMuted
                    result.success(null)
                }
                "recenter" -> {
                    navigationCamera?.requestNavigationCameraToFollowing()
                    sendEvent("recenterTriggered", null)
                    result.success(null)
                }
                "showRouteOverview" -> {
                    navigationCamera?.requestNavigationCameraToOverview()
                    sendEvent("routeOverviewTriggered", null)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
        initMapView()
    }


    private fun cacheRouteData(navigationRoute: NavigationRoute) {
        val geometryStr = navigationRoute.directionsRoute.geometry() ?: return
        val routeId = "route_cache_${navigationRoute.directionsRoute.hashCode()}"

        tileStore.getAllTileRegions { expected ->
            if (expected.isValue && expected.value?.any { it.id == routeId } == true) {
                Log.d(TAG, "Rota já em cache: $routeId")
                return@getAllTileRegions
            }

            val tilesetDescriptor = offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(NavigationStyles.NAVIGATION_DAY_STYLE)
                    .minZoom(0)
                    .maxZoom(15)
                    .build()
            )

            val routeGeometry = LineString.fromPolyline(geometryStr, 6)

            val options = TileRegionLoadOptions.Builder()
                .geometry(routeGeometry)
                .descriptors(listOf(tilesetDescriptor))
                .acceptExpired(true)
                .build()

            tileStore.loadTileRegion(routeId, options, { }, { expectedResult ->
                if (expectedResult.isValue) Log.d(TAG, "Corredor da rota salvo offline.")
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMapView() {
        _mapView = MapView(context)
        containerView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(_mapView)
        }

        _mapView?.mapboxMap?.loadStyle(NavigationStyles.NAVIGATION_DAY_STYLE) { style ->
            _mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .zoom(15.0)
                    .build()
            )
            _mapView?.logo?.enabled = false
            _mapView?.attribution?.enabled = false
            _mapView?.location?.apply {
                setLocationProvider(navigationLocationProvider)
                locationPuck = createDefault2DPuck(true)
                enabled = true
                puckBearingEnabled = true
            }
            _mapView?.gestures?.addOnMapClickListener { point ->
                val map = _mapView?.mapboxMap ?: return@addOnMapClickListener false
                lifecycleHelper?.lifecycleScope?.launch {
                    routeLineApi.findClosestRoute(point, map, 20f) { expected ->
                        expected.fold(
                            { error ->
                                Log.d(TAG, "Rota não encontrada: ${error}")
                            },
                            { closestRouteValue ->
                                selectNewPrimaryRoute(closestRouteValue.navigationRoute)
                            }
                        )
                    }
                }
                true
            }
            sendEvent("pluginInitialized", mapOf("viewId" to viewId))
            initializeNavigationComponents()
        } ?: run {
            Log.e(TAG, "MapView não foi inicializado corretamente ou falhou ao carregar o estilo.")
            sendEvent("error", mapOf("message" to "MapView failed to initialize or load style."))
        }
    }

    private fun selectNewPrimaryRoute(newPrimaryRoute: NavigationRoute) {
        val map = _mapView?.mapboxMap ?: return
        val style = map.getStyle() ?: return
        val allRoutes = routeLineApi.getNavigationRoutes().toMutableList()

        allRoutes.remove(newPrimaryRoute)
        allRoutes.add(0, newPrimaryRoute)

        routeLineApi.setNavigationRoutes(allRoutes) { value ->
            routeLineView.renderRouteDrawData(style, value)
        }
        mapboxNavigation?.setNavigationRoutes(allRoutes)
        currentDirectionsRoute = newPrimaryRoute.directionsRoute
        sendEvent("routeSelected", mapOf(
            "routeId" to newPrimaryRoute.directionsRoute.hashCode().toString(),
            "distance" to newPrimaryRoute.directionsRoute.distance(),
            "duration" to newPrimaryRoute.directionsRoute.duration()
        ))
        Log.d(TAG, "Nova rota selecionada via clique no mapa.")
    }

    override fun onFlutterViewAttached(flutterView: View) {
        val context = flutterView.context
        val shouldDestroyOnDestroy = when (context is FlutterActivity) {
            true -> context.shouldDestroyEngineWithHost()
            false -> true
        }
        lifecycleHelper = LifecycleHelper(lifecycleProvider, shouldDestroyOnDestroy)
        mapView?.setViewTreeLifecycleOwner(lifecycleHelper)
    }

    fun downloadRegionOffline(region: String, north: Double, east: Double, south: Double, west: Double) {

        val coordinates = listOf(
            listOf(
                Point.fromLngLat(west, north),
                Point.fromLngLat(east, north),
                Point.fromLngLat(east, south),
                Point.fromLngLat(west, south),
                Point.fromLngLat(west, north)
            )
        )
        val areaGeometry = Polygon.fromLngLats(coordinates)

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(NavigationStyles.NAVIGATION_DAY_STYLE)
                .minZoom(0)
                .maxZoom(15)
                .build()
        )


        val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .geometry(areaGeometry)
            .descriptors(listOf(tilesetDescriptor))
            .acceptExpired(true)
            .build()

        tileStore.loadTileRegion(
            region,
            tileRegionLoadOptions,
            { progress ->
                val percent = progress.completedResourceCount.toDouble() / progress.requiredResourceCount * 100
                sendEvent("offlineDownloadProgress", mapOf("percent" to percent))
            },
            { expected ->
                    if (expected.isError) {
                        Log.e(TAG, "Erro no download de $region: ${expected.error}")
                        sendEvent("error", mapOf("message" to expected.error.toString()))
                    } else {
                        Log.d(TAG, "Região $region baixada com sucesso!")
                        sendEvent("offlineDownloadComplete", mapOf("id" to region))
                    }
            }

        )
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initializeNavigationComponents() {
        if (MapboxOptions.accessToken.isEmpty()) {
            Log.e(TAG, "Chave de acesso do Mapbox não configurada.")
            sendEvent("error", mapOf("message" to "Chave de acesso do Mapbox não configurada."))
            return
        }

        MapboxNavigationApp.setup(
            NavigationOptions.Builder(context)
                .build()
        )
        MapboxNavigationApp.attach(lifecycleHelper!!)
        mapboxNavigation = MapboxNavigationApp.current()
        if (mapboxNavigation == null) {
            Log.e(TAG, "Falha ao inicializar MapboxNavigation.")
            sendEvent("error", mapOf("message" to "Falha ao inicializar MapboxNavigation."))
            return
        }

        viewportDataSource = MapboxNavigationViewportDataSource(_mapView!!.mapboxMap)
        viewportDataSource.options.apply {
            followingFrameOptions.apply {
                defaultPitch = 45.0
                minZoom = 12.0
                maxZoom = 18.0
                focalPoint = FollowingFrameOptions.FocalPoint(0.5, 1.0)
                pitchNearManeuvers.enabled = true
            }
            overviewFrameOptions.apply {
                maxZoom = 15.0
                pitchUpdatesAllowed = true
            }
        }

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
            viewportDataSource.followingPadding = followingPadding
        }
        navigationCamera = NavigationCamera(_mapView!!.mapboxMap, _mapView!!.camera, viewportDataSource)
        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(context)
                .routeLineBelowLayerId("road-label-navigation")
                .build()
        )
        routeArrowApi = MapboxRouteArrowApi()
        routeArrowView = MapboxRouteArrowView(RouteArrowOptions.Builder(context).build())
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(context).build()
        maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(context)
                .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
                .timeRemainingFormatter(TimeRemainingFormatter(context))
                .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
                .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(context))
                .build()
        )
        speechApi = MapboxSpeechApi(context, Locale.forLanguageTag("PT").language)
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(context, Locale.US.language)

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)


        mapboxNavigation?.startTripSession()
        sendEvent("mapboxNavigation", mapOf("isInitialized" to true))
        Log.d(TAG, "Componentes de navegação inicializados.")
    }


    private fun isValidCoordinate(value: Double, isLatitude: Boolean): Boolean {
        return if (isLatitude) value in -90.0..90.0 else value in -180.0..180.0
    }

    @SuppressLint("MissingPermission")
    fun createRoute(origin: List<Double>?, destination: List<Double>?, waypoints: List<List<Double>>?, isDestinationChange: Boolean = false) {
        val mapView = _mapView ?: return
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado.")
            sendEvent("error", mapOf("message" to "MapboxNavigation não está inicializado."))
            return
        }

        if (origin == null || destination == null || origin.size != 2 || destination.size != 2) {
            Log.e(TAG, "Coordenadas de origem ou destino inválidas: origin=$origin, destination=$destination")
            sendEvent("error", mapOf("message" to "Coordenadas de origem ou destino inválidas. Devem conter exatamente [latitude, longitude]."))
            return
        }
        val originLat = origin[0]
        val originLng = origin[1]
        val destLat = destination[0]
        val destLng = destination[1]

        if (!isValidCoordinate(originLat, true) || !isValidCoordinate(originLng, false) ||
            !isValidCoordinate(destLat, true) || !isValidCoordinate(destLng, false)) {
            Log.e(TAG, "Coordenadas fora do intervalo válido: origin=[$originLat, $originLng], destination=[$destLat, $destLng]")
            sendEvent("error", mapOf("message" to "Coordenadas fora do intervalo válido. Latitude: -90 a 90, Longitude: -180 a 180."))
            return
        }
        val originPoint = if (navigationLocationProvider.lastLocation != null) {
            Point.fromLngLat(
                navigationLocationProvider.lastLocation!!.longitude,
                navigationLocationProvider.lastLocation!!.latitude
            )
        } else {
            Log.w(TAG, "GPS lastLocation está null, a usar origin do Flutter como fallback.")
            Point.fromLngLat(origin[1], origin[0])
        }
        val destinationPoint = Point.fromLngLat(destLng, destLat)
        mapboxNavigation?.setNavigationRoutes(emptyList())
        mapboxNavigation?.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(originPoint, destinationPoint))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .steps(true)
                .voiceInstructions(true)
                .alternatives(true)
                .language("pt")
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    Log.d(TAG, "Cálculo de rota cancelado.")
                    sendEvent("routeCanceled", null)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    val message = reasons.joinToString(", ") { it.message }
                    Log.e(TAG, "Falha no cálculo de rota: $message")
                    sendEvent("error", mapOf("type" to "routeCalculationFailure", "message" to message))
                }

                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    if (routes.isNotEmpty()) {
                        mapboxNavigation?.setNavigationRoutes(routes)
                        navigationCamera?.requestNavigationCameraToOverview()
                        sendEvent("routeCreated", mapOf(
                            "routeId" to routes.first().directionsRoute.hashCode().toString(),
                            "routeCount" to routes.size,
                            "distance" to routes.first().directionsRoute.distance(),
                            "duration" to routes.first().directionsRoute.duration()
                        ))
                    } else {
                        Log.d(TAG, "Nenhuma rota encontrada.")
                        sendEvent("routeCreated", mapOf("routeCount" to 0))
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startNavigation(origin: List<Double>?, destination: List<Double>?, waypoints: List<List<Double>>?) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para iniciar a navegação.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for starting navigation."))
            return
        }

        if (currentDirectionsRoute == null) {
            if (origin == null || destination == null || origin.size != 2 || destination.size != 2) {
                Log.e(TAG, "Origem ou destino inválido: origin=$origin, destination=$destination")
                sendEvent("error", mapOf("message" to "Origem ou destino inválido. Devem conter exatamente [latitude, longitude]."))
                return
            }
            Log.d(TAG, "Rota não existe, tentando criar antes de iniciar a navegação.")
            createRoute(origin, destination, waypoints)
            return
        }

        setRouteAndStartNavigation()
        sendEvent("navigationStarted", null)
        Log.d(TAG, "Navegação iniciada.")
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun changeDestination(origin: List<Double>?,newDestination: List<Double>) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para mudar destino.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for changing destination."))
            return
        }

        if (newDestination.size != 2) {
            Log.e(TAG, "Coordenadas do novo destino inválidas: $newDestination")
            sendEvent("error", mapOf("message" to "Coordenadas do novo destino inválidas."))
            return
        }

        val newDestLat = newDestination[0]
        val newDestLng = newDestination[1]


        if (!isValidCoordinate(newDestLat, true) || !isValidCoordinate(newDestLng, false)) {
            Log.e(TAG, "Novo destino fora do intervalo válido: [$newDestLat, $newDestLng]")
            sendEvent("error", mapOf("message" to "Novo destino fora do intervalo válido. Latitude: -90 a 90, Longitude: -180 a 180."))
            return
        }
        val originPoint = if (origin?.size == 2) {
            origin.let { Point.fromLngLat(it[1], it[0]) }
        } else {
            Point.fromLngLat(
                navigationLocationProvider.lastLocation!!.longitude,
                navigationLocationProvider.lastLocation!!.latitude
            )
        }

        val destinationPoint = Point.fromLngLat(newDestLng, newDestLat)
        Log.d(TAG, "Destino alterado, recalculando rota.")
        if (originPoint != null) {
            mapboxNavigation?.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .coordinatesList(listOf(originPoint, destinationPoint))
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
                    .steps(true)
                    .voiceInstructions(true)
                    .alternatives(false)
                    .language("pt")
                    .build(),
                object : NavigationRouterCallback {
                    override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                        Log.d(TAG, "Cálculo de rota cancelado.")
                        sendEvent("routeCanceled", null)
                    }

                    override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                        val message = reasons.joinToString(", ") { it.message }
                        Log.e(TAG, "Falha no cálculo de rota: $message")
                        sendEvent("error", mapOf("type" to "routeCalculationFailure", "message" to message))
                    }

                    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                    override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                        if (routes.isNotEmpty()) {
                            mapboxNavigation?.setNavigationRoutes(routes)
                            setRouteAndStartNavigation()
                            sendEvent("routeCreated", mapOf(
                                "routeId" to routes.first().directionsRoute.hashCode().toString(),
                                "routeCount" to routes.size,
                                "distance" to routes.first().directionsRoute.distance(),
                                "duration" to routes.first().directionsRoute.duration()
                            ))
                        } else {
                            Log.d(TAG, "Nenhuma rota encontrada.")
                            sendEvent("routeCreated", mapOf("routeCount" to 0))
                        }
                    }
                }
            )
            sendEvent("destinationChanged", mapOf("newDestinationLat" to newDestLat, "newDestinationLng" to newDestLng))
            Log.d(TAG, "Destino alterado, recalculando rota.")
        } else {
            Log.e(TAG, "Não é possível mudar o destino, localização atual desconhecida.")
            sendEvent("error", mapOf("message" to "Current location unknown to change destination."))
        }
    }

    fun cancelNavigation() {
        //mapboxNavigation?.stopTripSession()
        mapboxNavigation?.setNavigationRoutes(emptyList())
        _mapView?.mapboxMap?.style?.let { style ->
            routeLineApi.cancel()
            routeLineView.cancel()
            routeArrowApi.clearArrows()
        }
        currentDirectionsRoute = null
        sendEvent("navigationCancelled", null)
        Log.d(TAG, "Navegação cancelada.")
    }

    fun finishNavigation() {
        cancelNavigation()
        sendEvent("navigationFinished", null)
        Log.d(TAG, "Navegação finalizada.")
    }

    fun stopNavigation() {
        cancelNavigation()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setRouteAndStartNavigation() {
        val currentRoutes = routeLineApi.getNavigationRoutes()
        if (currentRoutes.isNotEmpty()) {
            mapboxNavigation?.setNavigationRoutes(currentRoutes)
            navigationCamera?.requestNavigationCameraToFollowing()
            sendEvent("navigationStarted", null)
        }
        sendEvent("navigationStarted", null)
    }


    private fun sendEvent(type: String, data: Map<String, Any?>?) {
        val payload = data?.toMutableMap() ?: mutableMapOf()
        payload["type"] = type
        val jsonPayload = JSONObject(payload).toString()
        activityPluginBinding?.activity?.runOnUiThread {
            try {
                eventSink?.success(jsonPayload)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar evento para Flutter: $e")
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventSink) {
        this.eventSink = events
        Log.d(TAG, "EventChannel onListen para viewId $viewId")
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
        Log.d(TAG, "EventChannel onCancel para viewId $viewId")
    }

    override fun getView(): View {
        return containerView
    }

    @SuppressLint("MissingPermission")
    override fun dispose() {
        Log.d(TAG, "Disposing MapboxPlatformView com ID $viewId")
        MapboxViewManager.unregisterView(viewId)
        mapboxNavigation?.apply {
            unregisterRoutesObserver(routesObserver)
            unregisterLocationObserver(locationObserver)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            stopTripSession()
            setNavigationRoutes(emptyList())
            mapboxReplayer.stop()
            mapboxReplayer.clearEvents()
        }
        _mapView?.mapboxMap?.style?.let {
            routeLineApi.cancel()
            routeLineView.cancel()
            routeArrowApi.clearArrows()
        }
        _mapView?.gestures?.removeOnMapLongClickListener { true }
        maneuverApi.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
        //_mapView = null
        eventChannel.setStreamHandler(null)
        methodChannel.setMethodCallHandler(null)
        activityPluginBinding = null
        lifecycleHelper?.dispose()
        lifecycleHelper = null
        mapView?.setViewTreeLifecycleOwner(null)
        Log.d(TAG, "MapboxPlatformView com ID $viewId descartada e recursos liberados.")
    }
}

private class LifecycleHelper(
    val parentLifecycle: Lifecycle,
    val shouldDestroyOnDestroy: Boolean,
) : LifecycleOwner, DefaultLifecycleObserver {

    val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        parentLifecycle.addObserver(this)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate(owner: LifecycleOwner) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onStart(owner: LifecycleOwner) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onResume(owner: LifecycleOwner) {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onPause(owner: LifecycleOwner) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy(owner: LifecycleOwner) = propagateDestroyEvent()

    fun dispose() {
        parentLifecycle.removeObserver(this)
        propagateDestroyEvent()
    }

    private fun propagateDestroyEvent() {
        lifecycleRegistry.currentState = when (shouldDestroyOnDestroy) {
            true -> Lifecycle.State.DESTROYED
            false -> Lifecycle.State.CREATED
        }
    }
}