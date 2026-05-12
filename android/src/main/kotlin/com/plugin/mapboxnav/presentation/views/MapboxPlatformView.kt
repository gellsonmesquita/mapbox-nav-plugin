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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxOptions
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.common.OfflineSwitch
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.GlyphsRasterizationOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.TileStoreUsageMode
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.reroute.RerouteOptionsAdapter
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
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
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
import com.plugin.mapboxnav.core.config.MapboxConfig
import com.plugin.mapboxnav.domain.models.DataSaverMode
import com.plugin.mapboxnav.domain.models.NavigationBehaviorPolicy
import com.plugin.mapboxnav.domain.models.PerformancePolicy
import com.plugin.mapboxnav.domain.utils.EventRateLimiter
import com.plugin.mapboxnav.domain.utils.RouteRequestGate
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import com.plugin.mapboxnav.presentation.lifecycle.LifecycleHelper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.turf.TurfJoins

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxPlatformView(
    private val context: Context,
    private val messenger: BinaryMessenger,
    private val eventChannelBaseName: String,
    private val viewId: Int,
    private var activityPluginBinding: ActivityPluginBinding?,
    private val lifecycleProvider: Lifecycle,
    private val creationParams: Map<String, Any?>? = null,
) : PlatformView, EventChannel.StreamHandler, DefaultLifecycleObserver {

    private val TAG = "MapboxPlatformView"
    private var eventChannel: EventChannel
    private var eventSink: EventSink? = null
    private var methodChannel: MethodChannel

    private lateinit var containerView: View
    private var _mapView: MapView? = null
    val mapView: MapView? get() = _mapView
    private var isTruck = true
    private var maxHeight: Double? = null
    private var maxWeight: Double? = null
    private var maxWidth: Double? = null
    private var routeProfile = DirectionsCriteria.PROFILE_DRIVING
    private var routeLanguage = "pt-PT"
    private var routeUnits = DirectionsCriteria.METRIC
    private var routeGeometryPrecision = DirectionsCriteria.GEOMETRY_POLYLINE
    private var routeOverview = DirectionsCriteria.OVERVIEW_FULL
    private var routeAnnotations: List<String>? = null
    private var allowAlternatives = false
    private var enableRouteRefresh = false
    private var avoidList = mutableListOf<String>()
    private var forbiddenZones = mutableListOf<Polygon>()

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
    private var performancePolicy = PerformancePolicy()
    private var dataSaverMode = DataSaverMode.from(MapboxConfig.DEFAULT_DATA_SAVER_MODE)
    private var isWifiConnected: Boolean = true
    private var behaviorPolicy = NavigationBehaviorPolicy()
    private val locationEventLimiter = EventRateLimiter()
    private val tripProgressEventLimiter = EventRateLimiter()
    private val routeRequestGate = RouteRequestGate()
    private val offlineProgressLimiterByRegion = mutableMapOf<String, EventRateLimiter>()
    private val lastOfflineProgressPercentByRegion = mutableMapOf<String, Double>()
    private var pendingStartNavigation = false
    // Reference counter: network stays ON while any download is in progress.
    // Prevents race conditions when route calculation and cacheRouteData overlap.
    private val networkRefCount = java.util.concurrent.atomic.AtomicInteger(0)
    // Prevents duplicate acquires across reroute retries; released in routesObserver or cancelNavigation.
    private val rerouteNetworkAcquired = java.util.concurrent.atomic.AtomicBoolean(false)
    // True once at least one offline region (or route corridor) is confirmed in TileStore.
    // Used to decide whether to attempt offline-first routing before calling the Directions API.
    @Volatile private var hasDownloadedRegions = false
    // Tracks concurrent region downloads so the network isn't turned off while another download
    // is still in progress (relevant when the very first region is being downloaded).
    private val concurrentDownloadCount = java.util.concurrent.atomic.AtomicInteger(0)
    // Dynamic Wi-Fi detection — updated via ConnectivityManager callback.
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    // Bounding boxes of explicitly downloaded regions (persisted across sessions via SharedPrefs).
    // Used to detect when the user leaves a downloaded area so tiles can be served from network.
    private val downloadedRegionBounds = mutableMapOf<String, RegionBounds>()
    // True when the current position is outside all downloaded regions.
    // In this state the network stays ON (at low zoom) so the map is not blank.
    @Volatile private var isInUndownloadedArea = false
    private var lastAreaCheckMs = 0L
    private var isTripSessionActive = false
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            voiceInstructionsPlayer.volume(SpeechVolume(if (value) 0f else 1f))
            sendEvent("voiceInstructionsMuted", mapOf("muted" to value))
        }
    private var lifecycleHelper: LifecycleHelper? = null

    private var locationUpdateIntervalMs: Long = MapboxConfig.DEFAULT_LOCATION_UPDATE_INTERVAL_MS

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
        // Release network if an automatic reroute was in progress (acquire done in onRouteOptions).
        if (rerouteNetworkAcquired.getAndSet(false)) {
            releaseNetwork()
            Log.d(TAG, "Reroute: rede libertada")
        }
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
            _mapView ?: return
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            val activeNavigation = currentDirectionsRoute != null
            val baseIntervalMs = maxOf(performancePolicy.locationEventMinIntervalMs, locationUpdateIntervalMs)
            val effectiveIntervalMs = if (activeNavigation) baseIntervalMs else (baseIntervalMs * 3).coerceAtMost(15_000L)

            if (dataSaverMode == DataSaverMode.AGGRESSIVE && !activeNavigation) {
                return
            }

            if (locationEventLimiter.shouldEmit(effectiveIntervalMs)) {
                sendEvent("locationUpdate", mapOf(
                    "latitude" to enhancedLocation.latitude,
                    "longitude" to enhancedLocation.longitude,
                    "bearing" to enhancedLocation.bearing,
                    "accuracy" to enhancedLocation.bearingAccuracy,
                    "speed" to enhancedLocation.speed
                ))
            }
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()
            if (!firstLocationUpdateReceived && behaviorPolicy.autoFollowOnFirstLocation) {
                firstLocationUpdateReceived = true
                navigationCamera?.requestNavigationCameraToFollowing()
            }
            // Detect when the user crosses into/out of a downloaded region.
            // Throttled internally — safe to call on every location update.
            checkAreaStatus(enhancedLocation.latitude, enhancedLocation.longitude)
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
                        "instruction" to maneuver.primary.text,
                        "distance" to maneuver.stepDistance
                    )
                }
                sendEvent("maneuverUpdate", mapOf("maneuvers" to maneuverList))
            }
        )

        if (tripProgressEventLimiter.shouldEmit(performancePolicy.tripProgressEventMinIntervalMs)) {
            val tripProgress = tripProgressApi.getTripProgress(routeProgress)
            sendEvent("tripProgressUpdate", mapOf(
                "distanceRemaining" to tripProgress.distanceRemaining,
                "timeRemaining" to tripProgress.totalTimeRemaining,
                "percentRouteTraveled" to tripProgress.percentRouteTraveled,
                "estimatedTimeToArrival" to tripProgress.estimatedTimeToArrival
            ))
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        if (dataSaverMode == DataSaverMode.OFF) {
            // Cloud TTS (Mapbox API): melhor qualidade, usa rede por instrução
            speechApi.generate(voiceInstructions, speechCallback)
        } else {
            // Device TTS: sem chamada de rede, usa sintetizador local do Android
            val text = voiceInstructions.announcement() ?: return@VoiceInstructionsObserver
            voiceInstructionsPlayer.play(
                SpeechAnnouncement.Builder(text).build(),
                voiceInstructionsPlayerCallback
            )
        }
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
        // Only clean cloud TTS cache files — device TTS announcements have no cached audio file.
        if (dataSaverMode == DataSaverMode.OFF) {
            speechApi.clean(value)
        }
    }

    init {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkInfo = connectivityManager?.activeNetworkInfo
        isWifiConnected = networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI
        // Keep isWifiConnected in sync as the user switches between Wi-Fi and mobile data.
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: android.net.Network,
                caps: android.net.NetworkCapabilities
            ) {
                isWifiConnected = caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                Log.d(TAG, "Rede atualizada: isWifi=$isWifiConnected")
            }
            override fun onLost(network: android.net.Network) {
                isWifiConnected = false
                Log.d(TAG, "Rede perdida: isWifi=false")
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
        loadAllRegionBounds()
        eventChannel = EventChannel(messenger, "$eventChannelBaseName/$viewId")
        eventChannel.setStreamHandler(this)
        methodChannel = MethodChannel(messenger, "$eventChannelBaseName/$viewId/methods")
        applyCreationParamsDefaults()
        val fiveGigabytes = 5L * 1024 * 1024 * 1024
        tileStore.setOption(TileStoreOptions.DISK_QUOTA, Value(fiveGigabytes))
        // Share the same TileStore instance with the Maps SDK so downloaded regions are used
        MapboxMapsOptions.tileStore = tileStore
        // READ_ONLY: checks TileStore first (uses offline tiles when available), falls back to
        // individual tile download only for areas not yet downloaded — safe for partial coverage.
        // READ_AND_UPDATE would force tile-pack downloads for uncovered areas (worse for mobile data).
        MapboxMapsOptions.tileStoreUsageMode = TileStoreUsageMode.READ_ONLY
        methodChannel.setMethodCallHandler { call, result ->
            if (!handleMethodCall(call, result)) {
                result.notImplemented()
            }
        }
        initMapView()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun handleMethodCall(call: MethodCall, result: MethodChannel.Result): Boolean {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
                return true
            }
            "toggleVoiceInstructions" -> {
                isVoiceInstructionsMuted = !isVoiceInstructionsMuted
                result.success(null)
                return true
            }
            "setVoiceInstructionsMuted" -> {
                val muted = call.argument<Boolean>("muted")
                if (muted == null) {
                    result.error("INVALID_ARGS", "Argument 'muted' is required", null)
                } else {
                    isVoiceInstructionsMuted = muted
                    result.success(null)
                }
                return true
            }
            "recenter" -> {
                navigationCamera?.requestNavigationCameraToFollowing()
                sendEvent("recenterTriggered", null)
                result.success(null)
                return true
            }
            "showRouteOverview" -> {
                navigationCamera?.requestNavigationCameraToOverview()
                sendEvent("routeOverviewTriggered", null)
                result.success(null)
                return true
            }
            "updateRouteOptions" -> {
                applyRouteOptions(call.arguments as? Map<*, *>)
                result.success(null)
                return true
            }
            "setPerformancePolicy" -> {
                @Suppress("UNCHECKED_CAST")
                val args = call.arguments as? Map<String, Any>
                setPerformancePolicy(args)
                result.success(null)
                return true
            }
            "setDataSaverMode" -> {
                val modeName = call.argument<String>("mode")
                setDataSaverMode(modeName)
                result.success(null)
                return true
            }
            "setDataUsageConfig" -> {
                @Suppress("UNCHECKED_CAST")
                val args = call.arguments as? Map<String, Any>
                applyDataUsageConfig(args)
                result.success(null)
                return true
            }
            "setNavigationBehavior" -> {
                @Suppress("UNCHECKED_CAST")
                val args = call.arguments as? Map<String, Any>
                setNavigationBehavior(args)
                result.success(null)
                return true
            }
            "setTripSessionActive" -> {
                val active = call.argument<Boolean>("active")
                if (active == null) {
                    result.error("INVALID_ARGS", "Argument 'active' is required", null)
                } else {
                    setTripSessionActive(active)
                    result.success(null)
                }
                return true
            }
            "setForbiddenZones" -> {
                val zones = call.argument<List<List<Map<String, Double>>>>("zones")
                forbiddenZones.clear()
                zones?.forEach { polygonCoords ->
                    val points = polygonCoords.map { coord ->
                        Point.fromLngLat(coord["lng"] ?: 0.0, coord["lat"] ?: 0.0)
                    }
                    if (points.isNotEmpty()) {
                        forbiddenZones.add(Polygon.fromLngLats(listOf(points)))
                    }
                }
                Log.d(TAG, "Zonas proibidas atualizadas: ${forbiddenZones.size} zonas carregadas.")
                result.success(null)
                return true
            }
            "createRoute" -> {
                val origin = call.argument<List<Double>>("origin")
                val destination = call.argument<List<Double>>("destination")
                val waypoints = call.argument<List<List<Double>>>("waypoints")
                if (origin == null || destination == null) {
                    result.error("MISSING_ARG", "Origin and destination are required", null)
                } else {
                    createRoute(origin, destination, waypoints)
                    result.success(true)
                }
                return true
            }
            "startNavigation" -> {
                val origin = call.argument<List<Double>>("origin")
                val destination = call.argument<List<Double>>("destination")
                val waypoints = call.argument<List<List<Double>>>("waypoints")
                if (destination == null) {
                    result.error("MISSING_ARG", "Destination is required", null)
                } else {
                    startNavigation(origin, destination, waypoints)
                    result.success(true)
                }
                return true
            }
            "changeDestination" -> {
                val newDestination = call.argument<List<Double>>("newDestination")
                val origin = call.argument<List<Double>>("origin")
                if (newDestination == null) {
                    result.error("MISSING_ARG", "New destination is required", null)
                } else {
                    changeDestination(origin, newDestination)
                    result.success(true)
                }
                return true
            }
            "downloadOfflineArea" -> {
                val args = call.arguments as? Map<*, *>
                val region = args?.get("region") as? String
                val north = (args?.get("north") as? Number)?.toDouble()
                val east = (args?.get("east") as? Number)?.toDouble()
                val south = (args?.get("south") as? Number)?.toDouble()
                val west = (args?.get("west") as? Number)?.toDouble()
                val maxZoom = (args?.get("maxZoom") as? Number)?.toInt() ?: 17
                if (region == null || north == null || east == null || south == null || west == null) {
                    result.error("MISSING_ARG", "region, north, east, south and west are required", null)
                } else {
                    downloadRegionOffline(region, north, east, south, west, maxZoom)
                    result.success(null)
                }
                return true
            }
            "listOfflineRegions" -> {
                tileStore.getAllTileRegions { expected ->
                    if (expected.isError) {
                        result.error("TILE_STORE_ERROR", expected.error.toString(), null)
                    } else {
                        val regions = expected.value?.map { region ->
                            mapOf(
                                "id" to region.id,
                                "completedBytes" to region.completedResourceSize,
                                "completedCount" to region.completedResourceCount,
                                "requiredCount" to region.requiredResourceCount,
                            )
                        } ?: emptyList()
                        result.success(regions)
                    }
                }
                return true
            }
            "deleteOfflineRegion" -> {
                val region = call.argument<String>("region")
                if (region == null) {
                    result.error("MISSING_ARG", "region is required", null)
                } else {
                    tileStore.removeTileRegion(region)
                    removeRegionBounds(region)
                    Log.d(TAG, "Região $region removida.")
                    result.success(null)
                }
                return true
            }
            "toggleTraffic" -> {
                val show = call.argument<Boolean>("show") ?: true
                toggleTraffic(show)
                result.success(null)
                return true
            }
            "cancelNavigation" -> {
                cancelNavigation()
                result.success(true)
                return true
            }
            "finishNavigation" -> {
                finishNavigation()
                result.success(true)
                return true
            }
            "stopNavigation" -> {
                stopNavigation()
                result.success(true)
                return true
            }
            else -> return false
        }
    }

    private fun applyCreationParamsDefaults() {
        val params = creationParams ?: emptyMap<String, Any?>()
        val initialMode = params["dataSaverMode"] as? String ?: MapboxConfig.DEFAULT_DATA_SAVER_MODE
        setDataSaverMode(initialMode)

        @Suppress("UNCHECKED_CAST")
        val routeArgs = params["routeOptions"] as? Map<*, *>
        applyRouteOptions(routeArgs)

        @Suppress("UNCHECKED_CAST")
        val perfArgs = params["performancePolicy"] as? Map<String, Any>
        if (perfArgs != null) {
            setPerformancePolicy(perfArgs)
        }

        @Suppress("UNCHECKED_CAST")
        val behaviorArgs = params["navigationBehavior"] as? Map<String, Any>
        if (behaviorArgs != null) {
            behaviorPolicy = NavigationBehaviorPolicy.fromMap(behaviorArgs)
        }

        locationUpdateIntervalMs =
            (params["locationUpdateIntervalMs"] as? Number)?.toLong() ?: locationUpdateIntervalMs
    }

    private fun applyRouteOptions(args: Map<*, *>?) {
        isTruck = args?.get("isTruck") as? Boolean ?: isTruck
        maxHeight = (args?.get("maxHeight") as? Number)?.toDouble() ?: maxHeight
        maxWeight = (args?.get("maxWeight") as? Number)?.toDouble() ?: maxWeight
        maxWidth = (args?.get("maxWidth") as? Number)?.toDouble() ?: maxWidth
        routeProfile = args?.get("profile") as? String ?: routeProfile
        routeLanguage = args?.get("language") as? String ?: routeLanguage
        routeUnits = args?.get("units") as? String ?: routeUnits
        routeGeometryPrecision = args?.get("geometryPrecision") as? String ?: routeGeometryPrecision
        routeOverview = args?.get("overview") as? String ?: routeOverview
        allowAlternatives = args?.get("alternatives") as? Boolean ?: allowAlternatives
        enableRouteRefresh = args?.get("enableRefresh") as? Boolean ?: enableRouteRefresh
        @Suppress("UNCHECKED_CAST")
        val flutterAnnotations = args?.get("annotations") as? List<Any?>
        if (flutterAnnotations != null) {
            routeAnnotations = flutterAnnotations.mapNotNull { it?.toString() }
        }
        @Suppress("UNCHECKED_CAST")
        val flutterExclusions = args?.get("excludeList") as? List<String>
        if (flutterExclusions != null) {
            avoidList = flutterExclusions.toMutableList()
        }
    }

    private fun applyDataUsageConfig(args: Map<String, Any>?) {
        if (args == null) return
        setDataSaverMode(args["mode"] as? String)
        allowAlternatives = args["allowAlternatives"] as? Boolean ?: allowAlternatives
        enableRouteRefresh = args["enableRouteRefresh"] as? Boolean ?: enableRouteRefresh
        routeGeometryPrecision = args["geometryPrecision"] as? String ?: routeGeometryPrecision
        locationUpdateIntervalMs =
            (args["locationUpdateIntervalMs"] as? Number)?.toLong() ?: locationUpdateIntervalMs

        @Suppress("UNCHECKED_CAST")
        val perfArgs = args["performancePolicy"] as? Map<String, Any>
        if (perfArgs != null) {
            performancePolicy = PerformancePolicy.fromMap(perfArgs)
        }

        sendEvent(
            "dataUsageConfigChanged",
            mapOf(
                "mode" to dataSaverMode.name,
                "locationUpdateIntervalMs" to locationUpdateIntervalMs,
                "allowAlternatives" to allowAlternatives,
                "enableRouteRefresh" to enableRouteRefresh,
                "geometryPrecision" to routeGeometryPrecision,
            )
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun applyTripSessionBehavior() {
        if (behaviorPolicy.autoStartTripSession) {
            mapboxNavigation?.startTripSession()
            isTripSessionActive = true
        } else {
            mapboxNavigation?.stopTripSession()
            isTripSessionActive = false
        }
    }

    fun updateVoiceInstructionsMuted(muted: Boolean) {
        isVoiceInstructionsMuted = muted
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun setTripSessionActive(active: Boolean) {
        if (active) {
            mapboxNavigation?.startTripSession()
            isTripSessionActive = true
        } else {
            mapboxNavigation?.stopTripSession()
            isTripSessionActive = false
        }
        sendEvent("tripSessionStateChanged", mapOf("active" to active))
    }

    fun setPerformancePolicy(args: Map<String, Any>?) {
        performancePolicy = PerformancePolicy.fromMap(args)
    }

    fun setDataSaverMode(modeName: String?) {
        val mode = DataSaverMode.from(modeName)
        dataSaverMode = mode
        when (mode) {
            DataSaverMode.OFF -> {
                allowAlternatives = false
                enableRouteRefresh = false
                performancePolicy = PerformancePolicy()
                locationUpdateIntervalMs = 1000L
            }
            DataSaverMode.BALANCED -> {
                allowAlternatives = false
                enableRouteRefresh = false
                performancePolicy = PerformancePolicy(
                    routeRequestCooldownMs = 10_000,
                    locationEventMinIntervalMs = 1_500,
                    tripProgressEventMinIntervalMs = 2_000,
                    offlineProgressEventMinIntervalMs = 1_500,
                    skipDuplicateRouteRequests = true,
                )
                locationUpdateIntervalMs = 3000L
            }
            DataSaverMode.AGGRESSIVE -> {
                allowAlternatives = false
                enableRouteRefresh = false
                performancePolicy = PerformancePolicy(
                    routeRequestCooldownMs = 30_000,
                    locationEventMinIntervalMs = 3_000,
                    tripProgressEventMinIntervalMs = 5_000,
                    offlineProgressEventMinIntervalMs = 3_000,
                    skipDuplicateRouteRequests = true,
                )
                locationUpdateIntervalMs = 6000L
            }
        }
        applyMapDataSaverConfig()
        sendEvent("dataSaverModeChanged", mapOf("mode" to mode.name))
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun setNavigationBehavior(args: Map<String, Any>?) {
        behaviorPolicy = NavigationBehaviorPolicy.fromMap(args)
        applyTripSessionBehavior()
    }

    private fun cacheRouteData(navigationRoute: NavigationRoute) {
        // Corridor download only uses mobile data in OFF mode with no tiles downloaded yet.
        // In every other case (BALANCED, AGGRESSIVE, or OFF+tiles) only WiFi is allowed —
        // skip early so we never call acquireNetwork() and briefly open OfflineSwitch on
        // mobile data, which would let the entire Mapbox SDK consume data during that window.
        val allowMobileData = dataSaverMode == DataSaverMode.OFF && !hasDownloadedRegions
        if (!isWifiConnected && !allowMobileData) return

        val geometryStr = navigationRoute.directionsRoute.geometry() ?: return
        // Use destination coordinates (rounded to ~10 m) as cache key so that reroutes to the
        // same destination reuse the existing tile region instead of re-downloading it every time.
        val dest = navigationRoute.directionsRoute.legs()
            ?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
        val routeId = if (dest != null) {
            val latKey = Math.round(dest.latitude() * 10_000)
            val lngKey = Math.round(dest.longitude() * 10_000)
            "route_cache_${latKey}_${lngKey}"
        } else {
            "route_cache_${navigationRoute.directionsRoute.hashCode()}"
        }

        tileStore.getAllTileRegions { expected ->
            if (expected.isValue && expected.value?.any { it.id == routeId } == true) {
                Log.d(TAG, "Corredor já em cache: $routeId")
                return@getAllTileRegions
            }

            val routeGeometry = LineString.fromPolyline(geometryStr, 6)

            // In BALANCED/AGGRESSIVE: download only routing tiles (1–5 MB) — used for offline
            // rerouting along the corridor. Map tiles (10–50 MB) are skipped because:
            //   • Within a downloaded region: already present, deduplication = 0 MB.
            //   • Outside a downloaded region: map renders blank but navigation still works.
            // In OFF mode: also include map tiles for full visual fidelity.
            val mapDescriptor = if (dataSaverMode == DataSaverMode.OFF) {
                offlineManager.createTilesetDescriptor(
                    TilesetDescriptorOptions.Builder()
                        .styleURI(NavigationStyles.NAVIGATION_DAY_STYLE)
                        .minZoom(13)
                        .maxZoom(20)
                        .build()
                )
            } else null

            val routingDescriptor = mapboxNavigation?.tilesetDescriptorFactory?.getLatest()

            val descriptors = listOfNotNull(mapDescriptor, routingDescriptor)
            if (descriptors.isEmpty()) return@getAllTileRegions

            // We only reach here when WiFi is available (or OFF+noTiles on mobile).
            // Use NONE because we already guaranteed the correct network is present above.
            val networkRestriction = if (allowMobileData)
                com.mapbox.common.NetworkRestriction.NONE
            else
                com.mapbox.common.NetworkRestriction.DISALLOW_EXPENSIVE

            val options = TileRegionLoadOptions.Builder()
                .geometry(routeGeometry)
                .descriptors(descriptors)
                .networkRestriction(networkRestriction)
                .acceptExpired(true)
                .build()

            acquireNetwork()
            Log.d(TAG, "cacheRouteData START modo=$dataSaverMode descritores=${descriptors.size}")
            tileStore.loadTileRegion(routeId, options,
                { progress ->
                    Log.d(TAG, "cacheRouteData PROGRESS tiles=${progress.completedResourceCount}/${progress.requiredResourceCount} bytes=${progress.completedResourceSize}/${progress}")
                },
                { expectedResult ->
                    releaseNetwork()
                    if (expectedResult.isValue) {
                        hasDownloadedRegions = true
                        val region = expectedResult.value!!
                        Log.d(TAG, "cacheRouteData DONE tiles=${region.completedResourceCount} bytes=${region.completedResourceSize} modo=$dataSaverMode")
                    } else {
                        Log.e(TAG, "cacheRouteData ERRO ${expectedResult.error}")
                    }
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMapView() {
        val glyphsOptions = GlyphsRasterizationOptions.Builder()
            // ALL_GLYPHS: renders ALL text (including Portuguese street names) using device fonts.
            // Eliminates network requests to api.mapbox.com/fonts/v1/... for every tile render.
            // IDEOGRAPHS mode only covers CJK text — Latin script still fetched from network.
            .rasterizationMode(GlyphsRasterizationMode.ALL_GLYPHS_RASTERIZED_LOCALLY)
            .build()
        val mapOptions = MapOptions.Builder()
            .glyphsRasterizationOptions(glyphsOptions)
            .build()
        val mapInitOptions = MapInitOptions(
            context = context,
            mapOptions = mapOptions,
            styleUri = NavigationStyles.NAVIGATION_DAY_STYLE
        )
        _mapView = MapView(context, mapInitOptions)
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
                    .zoom(17.0)
                    .build()
            )
            val sourceIds = style.styleSources.map { it.id }
            Log.d(TAG, "Style sources loaded: $sourceIds")
            applyMapDataSaverConfig()
            if (dataSaverMode != DataSaverMode.OFF) {
                removeRealtimeSources(style)
            }
            // Disable network whenever offline tiles exist, regardless of dataSaverMode.
            // When tiles are downloaded the SDK must use them instead of consuming mobile data.
            // Network is re-enabled only for specific operations (route request, tile download).
            tileStore.getAllTileRegions { expected ->
                val hasRegions = expected.isValue && expected.value?.isNotEmpty() == true
                hasDownloadedRegions = hasRegions
                if (hasRegions) {
                    setMapboxNetwork(false)
                    Log.d(TAG, "Regioes encontradas — rede desligada")
                } else {
                    Log.d(TAG, "Sem regioes baixadas — rede mantida activa")
                }
            }
            _mapView?.logo?.enabled = false
            _mapView?.attribution?.enabled = false
            _mapView?.location?.apply {
                setLocationProvider(navigationLocationProvider)
                // mapbox_navigation_puck_icon2 is the official Mapbox Navigation SDK puck:
                // a large, visible navigation arrow with shadow, identical to the Mapbox
                // navigation app. Rotates with COURSE (direction of movement, not compass).
                locationPuck = LocationPuck2D(
                    bearingImage = ImageHolder.from(com.plugin.mapboxnav.R.drawable.nav_puck_icon),
                    shadowImage = ImageHolder.from(
                        com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon2_shadow
                    )
                )
                puckBearing = PuckBearing.COURSE
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

    fun downloadRegionOffline(region: String, north: Double, east: Double, south: Double, west: Double, maxZoom: Int = 17) {
        // Track concurrent downloads so we don't turn the network off while another is in progress.
        concurrentDownloadCount.incrementAndGet()
        // acquireNetwork is a no-op when !hasDownloadedRegions (network is always ON in that state).
        // When regions already exist we need the ref count to stay balanced.
        acquireNetwork()

        tileStore.getAllTileRegions { expected ->
            if (expected.isValue) {
                val existingRegions = expected.value ?: emptyList()
                if (existingRegions.any { it.id == region }) {
                    Log.d(TAG, "A região $region já está baixada. Ignorando download.")
                    concurrentDownloadCount.decrementAndGet()
                    releaseNetwork()
                    sendEvent("offlineDownloadComplete", mapOf("id" to region, "status" to "already_exists"))
                    return@getAllTileRegions
                }
            }

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

            val clampedMaxZoom: Byte = maxZoom.coerceIn(13, 20).toByte()
            val mapTilesetDescriptor = offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(NavigationStyles.NAVIGATION_DAY_STYLE)
                    .minZoom(13)
                    .maxZoom(clampedMaxZoom)
                    .build()
            )
            // Routing tiles: enable offline route calculation within this region (no Directions API call)
            val routingTilesetDescriptor = mapboxNavigation?.tilesetDescriptorFactory?.getLatest()

            val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
                .geometry(areaGeometry)
                .descriptors(listOfNotNull(mapTilesetDescriptor, routingTilesetDescriptor))
                .acceptExpired(true)
                .build()

            tileStore.loadTileRegion(
                region,
                tileRegionLoadOptions,
                { progress ->
                    val total = progress.requiredResourceCount.toDouble()
                    val percent = if (total > 0) (progress.completedResourceCount / total) * 100 else 0.0
                    if (shouldEmitOfflineProgress(region, percent)) {
                        sendEvent("offlineDownloadProgress", mapOf("id" to region, "percent" to percent))
                    }
                },
                { expected ->
                    val remainingDownloads = concurrentDownloadCount.decrementAndGet()
                    if (expected.isError) {
                        Log.e(TAG, "Erro no download de $region: ${expected.error}")
                        releaseNetwork()
                        sendEvent("error", mapOf("message" to expected.error.toString()))
                    } else {
                        val wasFirstDownload = !hasDownloadedRegions
                        // Set hasDownloadedRegions BEFORE releaseNetwork so the ref-count logic
                        // in releaseNetwork is active and correctly transitions to managed state.
                        hasDownloadedRegions = true
                        // Persist bounds so the app knows where this region is in future sessions.
                        saveRegionBounds(region, north, east, south, west)
                        Log.d(TAG, "Região $region baixada com sucesso!")
                        sendEvent("offlineDownloadComplete", mapOf("id" to region))
                        if (wasFirstDownload) {
                            // Transition: network was unmanaged (always ON). Switch to managed
                            // mode only after ALL concurrent downloads finish to avoid cutting
                            // the network while another download is still in progress.
                            if (remainingDownloads == 0 && networkRefCount.get() == 0) {
                                setMapboxNetwork(false)
                                Log.d(TAG, "Primeiro download concluído — iniciando gestão da rede")
                            }
                        } else {
                            releaseNetwork()
                        }
                    }
                }
            )
        }
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initializeNavigationComponents() {
        if (MapboxOptions.accessToken.isEmpty()) {
            Log.e(TAG, "Chave de acesso do Mapbox não configurada.")
            sendEvent("error", mapOf("message" to "Chave de acesso do Mapbox não configurada."))
            return
        }

        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(context)
                    .routingTilesOptions(
                        // Use the same TileStore so routing tiles downloaded offline are reused
                        RoutingTilesOptions.Builder()
                            .tileStore(tileStore)
                            .build()
                    )
                    .build()
            )
        }
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
                minZoom = 16.0
                // 0.75 keeps the puck in the lower third without pushing it to the edge (which
                // combined with high pitch made the view look "from the side").
                focalPoint = FollowingFrameOptions.FocalPoint(0.5, 0.75)
                pitchNearManeuvers.enabled = true
                bearingUpdatesAllowed = true
            }
            overviewFrameOptions.apply {
                pitchUpdatesAllowed = true
            }
        }
        // Apply mode-specific zoom and pitch after viewportDataSource is ready
        applyMapDataSaverConfig()

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
            viewportDataSource.followingPadding = followingPadding
        }
        navigationCamera = NavigationCamera(_mapView!!.mapboxMap, _mapView!!.camera, viewportDataSource)
        navigationCamera!!.registerNavigationCameraStateChangeObserver { _ -> }
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
        speechApi = MapboxSpeechApi(context, routeLanguage)
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(context, routeLanguage)

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.setRerouteOptionsAdapter(object : RerouteOptionsAdapter {
            override fun onRouteOptions(routeOptions: RouteOptions): RouteOptions {
                // If offline routing tiles are available, keep the network off — the SDK will
                // calculate the reroute locally.  Only acquire network when there are no local
                // tiles (first reroute ever, or outside any downloaded region).
                val useOfflineReroute = hasDownloadedRegions
                if (!useOfflineReroute) {
                    // compareAndSet: only first call per reroute acquires network (SDK may retry).
                    if (rerouteNetworkAcquired.compareAndSet(false, true)) {
                        acquireNetwork()
                        Log.d(TAG, "Reroute: rede adquirida (sem tiles locais)")
                    }
                } else {
                    Log.d(TAG, "Reroute: usando tiles locais — sem chamada à API")
                }
                val preferredOverview = when (dataSaverMode) {
                    DataSaverMode.OFF -> routeOverview
                    DataSaverMode.BALANCED, DataSaverMode.AGGRESSIVE -> {
                        if (routeOverview == DirectionsCriteria.OVERVIEW_FULL) DirectionsCriteria.OVERVIEW_FULL
                        else DirectionsCriteria.OVERVIEW_SIMPLIFIED
                    }
                }
                val (effectiveOverview, effectiveAnnotations) = resolveOverviewAndAnnotations(preferredOverview)
                return routeOptions.toBuilder()
                    .alternatives(false)
                    .enableRefresh(false)
                    .overview(effectiveOverview)
                    .annotationsList(effectiveAnnotations)
                    .geometries(
                        if (dataSaverMode == DataSaverMode.OFF) routeGeometryPrecision
                        else DirectionsCriteria.GEOMETRY_POLYLINE
                    )
                    // steps=true always required: Navigation SDK v3 mandates step data.
                    .steps(true)
                    // voice_instructions=true always required: announcement() text comes from route;
                    // without it device TTS has nothing to speak after reroute.
                    .voiceInstructions(true)
                    .build()
            }
        })



        applyTripSessionBehavior()
        sendEvent("mapboxNavigation", mapOf("isInitialized" to true))
        Log.d(TAG, "Componentes de navegação inicializados.")
    }

    private fun isValidCoordinate(value: Double, isLatitude: Boolean): Boolean {
        return if (isLatitude) value in -90.0..90.0 else value in -180.0..180.0
    }

    @SuppressLint("MissingPermission")
    fun createRoute(origin: List<Double>?, destination: List<Double>?, waypointsList: List<List<Double>>?, isDestinationChange: Boolean = false) {
        if (mapboxNavigation == null || origin == null || destination == null) return
        if (origin.size != 2 || destination.size != 2) {
            sendEvent("error", mapOf("message" to "Origem e destino devem conter [latitude, longitude]."))
            return
        }
        if (!isValidCoordinate(origin[0], true) || !isValidCoordinate(origin[1], false) ||
            !isValidCoordinate(destination[0], true) || !isValidCoordinate(destination[1], false)
        ) {
            sendEvent("error", mapOf("message" to "Coordenadas inválidas. Verifique latitude/longitude."))
            return
        }

        val originPoint = if (navigationLocationProvider.lastLocation != null) {
            Point.fromLngLat(navigationLocationProvider.lastLocation!!.longitude, navigationLocationProvider.lastLocation!!.latitude)
        } else {
            Point.fromLngLat(origin[1], origin[0])
        }

        val destinationPoint = Point.fromLngLat(destination[1], destination[0])
        val waypoints = waypointsList?.map { Point.fromLngLat(it[1], it[0]) }

        val options = buildRouteOptions(originPoint, destinationPoint, waypoints)
        val routeSignature = buildRouteSignature(originPoint, destinationPoint, waypoints)
        val skipReason = routeRequestGate.skipReason(routeSignature, performancePolicy)
        if (skipReason != null) {
            sendEvent("routeRequestSkipped", mapOf("reason" to skipReason.name.lowercase()))
            return
        }
        routeRequestGate.markInFlight(routeSignature)

        // Always query TileStore at call time for a fresh tile availability check.
        // Prefer offline routing when tiles exist — avoids a Directions API call regardless
        // of dataSaverMode. Falls back to the API transparently when tiles don't cover the area.
        tileStore.getAllTileRegions { expected ->
            val hasRegions = expected.isValue && expected.value?.isNotEmpty() == true
            hasDownloadedRegions = hasRegions
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (!hasRegions) acquireNetwork()
                doRequestRoutes(options, isDestinationChange, tryOffline = hasRegions)
            }
        }
    }

    private fun doRequestRoutes(
        options: RouteOptions,
        isDestinationChange: Boolean,
        tryOffline: Boolean,
    ) {
        mapboxNavigation?.requestRoutes(options, object : NavigationRouterCallback {
            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                if (!tryOffline) releaseNetwork()
                routeRequestGate.clearInFlight()
                pendingStartNavigation = false
                sendEvent("routeCanceled", null)
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                if (tryOffline) {
                    // Local tiles don't cover this route — retry transparently via Directions API.
                    Log.d(TAG, "Routing offline falhou (${reasons.joinToString { it.message }}), a tentar via API")
                    acquireNetwork()
                    mapboxNavigation?.requestRoutes(routeOptions, object : NavigationRouterCallback {
                        override fun onCanceled(ro: RouteOptions, origin: String) {
                            releaseNetwork(); routeRequestGate.clearInFlight()
                            pendingStartNavigation = false; sendEvent("routeCanceled", null)
                        }
                        override fun onFailure(r: List<RouterFailure>, ro: RouteOptions) {
                            releaseNetwork(); routeRequestGate.clearInFlight()
                            pendingStartNavigation = false
                            sendEvent("error", mapOf("type" to "routeCalculationFailure", "message" to r.joinToString(", ") { it.message }))
                        }
                        override fun onRoutesReady(routes: List<NavigationRoute>, origin: String) {
                            releaseNetwork(); routeRequestGate.clearInFlight()
                            Log.d(TAG, "Route origin (API fallback): $origin")
                            onRoutesReadyImpl(routes, isDestinationChange)
                        }
                    })
                } else {
                    releaseNetwork()
                    routeRequestGate.clearInFlight()
                    pendingStartNavigation = false
                    val message = reasons.joinToString(", ") { it.message }
                    sendEvent("error", mapOf("type" to "routeCalculationFailure", "message" to message))
                }
            }

            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                if (!tryOffline) releaseNetwork()
                routeRequestGate.clearInFlight()
                Log.d(TAG, "Route origin: $routerOrigin")
                onRoutesReadyImpl(routes, isDestinationChange)
            }
        })
    }

    private fun onRoutesReadyImpl(routes: List<NavigationRoute>, isDestinationChange: Boolean) {
        val validRoutes = routes.filter { !isRouteInForbiddenZone(it.directionsRoute) }
        if (validRoutes.isNotEmpty()) {
            mapboxNavigation?.setNavigationRoutes(validRoutes)
            if (behaviorPolicy.autoOverviewOnRouteReady) {
                navigationCamera?.requestNavigationCameraToOverview()
            }
            sendEvent("routeCreated", mapOf(
                "routeId" to validRoutes.first().directionsRoute.hashCode().toString(),
                "routeCount" to validRoutes.size,
                "distance" to validRoutes.first().directionsRoute.distance(),
                "duration" to validRoutes.first().directionsRoute.duration()
            ))
            if (isDestinationChange || pendingStartNavigation) {
                pendingStartNavigation = false
                // Pass validRoutes directly: routesObserver may fire async so
                // routeLineApi.getNavigationRoutes() could still be empty here.
                setRouteAndStartNavigation(validRoutes)
                if (behaviorPolicy.autoFollowOnDestinationChange) {
                    navigationCamera?.requestNavigationCameraToFollowing()
                }
            }
        } else {
            pendingStartNavigation = false
            sendEvent("routeCreated", mapOf("routeCount" to 0))
        }
    }

    @SuppressLint("MissingPermission")
    fun startNavigation(origin: List<Double>?, destination: List<Double>?, waypoints: List<List<Double>>?) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para iniciar a navegação.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for starting navigation."))
            return
        }

        if (currentDirectionsRoute == null || routeLineApi.getNavigationRoutes().isEmpty()) {
            if (origin == null || destination == null || origin.size != 2 || destination.size != 2) {
                Log.e(TAG, "Origem ou destino inválido: origin=$origin, destination=$destination")
                sendEvent("error", mapOf("message" to "Origem ou destino inválido. Devem conter exatamente [latitude, longitude]."))
                return
            }
            Log.d(TAG, "Rota não existe ou routeLineApi vazio, criando rota antes de iniciar.")
            pendingStartNavigation = true
            createRoute(origin, destination, waypoints)
            return
        }

        setRouteAndStartNavigation()
        Log.d(TAG, "Navegação iniciada.")
    }

    private fun setMapboxNetwork(connected: Boolean) {
        OfflineSwitch.getInstance().setMapboxStackConnected(connected)
        Log.d(TAG, "Mapbox network: ${if (connected) "ON" else "OFF"}")
    }

    // Increment reference count — network turns ON on first acquire.
    private fun acquireNetwork() {
        // When no regions are downloaded there is nothing to save: the whole app depends on
        // the network for map tiles and routing. Leave OfflineSwitch untouched (stays ON).
        // Network management only starts after the first region has been downloaded.
        if (!hasDownloadedRegions) return
        if (networkRefCount.incrementAndGet() == 1) setMapboxNetwork(true)
        Log.d(TAG, "acquireNetwork refs=${networkRefCount.get()}")
    }

    // Decrement reference count — network turns OFF when last release is called,
    // unless the user is currently outside all downloaded regions (tiles still need network).
    private fun releaseNetwork() {
        // Mirror of acquireNetwork: no-op until regions exist.
        if (!hasDownloadedRegions) return
        val refs = networkRefCount.decrementAndGet().coerceAtLeast(0)
        networkRefCount.set(refs)
        if (refs == 0 && !isInUndownloadedArea) setMapboxNetwork(false)
        Log.d(TAG, "releaseNetwork refs=$refs inUndownloaded=$isInUndownloadedArea")
    }

    private fun applyMapDataSaverConfig() {
        val map = _mapView?.mapboxMap ?: return
        val vpOptions = if (::viewportDataSource.isInitialized) viewportDataSource.options else null
        when (dataSaverMode) {
            DataSaverMode.OFF -> {
                map.setPrefetchZoomDelta(4)
                toggleTraffic(true)
                vpOptions?.followingFrameOptions?.apply {
                    defaultPitch = 45.0
                    maxZoom = 20.0
                }
                vpOptions?.overviewFrameOptions?.maxZoom = 20.0
            }
            DataSaverMode.BALANCED -> {
                map.setPrefetchZoomDelta(0)
                toggleTraffic(false)
                vpOptions?.followingFrameOptions?.apply {
                    maxZoom = 18.0
                    defaultPitch = 40.0
                }
                vpOptions?.overviewFrameOptions?.maxZoom = 14.0
            }
            DataSaverMode.AGGRESSIVE -> {
                map.setPrefetchZoomDelta(0)
                toggleTraffic(false)
                vpOptions?.followingFrameOptions?.apply {
                    maxZoom = 17.0
                    defaultPitch = 40.0
                }
                vpOptions?.overviewFrameOptions?.maxZoom = 14.0
            }
        }
    }

    fun toggleTraffic(show: Boolean) {
        _mapView?.mapboxMap?.getStyle() { style ->
            val visibility = if (show) Visibility.VISIBLE else Visibility.NONE
            // Navigation Day Style uses several traffic layers (traffic-low, traffic-moderate,
            // traffic-heavy, traffic-severe, etc.). Iterate all and hide/show by name match.
            style.styleLayers
                .filter { it.id.contains("traffic", ignoreCase = true) }
                .forEach { layerInfo -> style.getLayer(layerInfo.id)?.visibility(visibility) }
        }
    }

    private val realtimeSourceKeywords = listOf("traffic", "incident", "hazard", "event", "congestion")

    private fun removeRealtimeSources(style: com.mapbox.maps.Style) {
        // Identify real-time sources by keyword match and log them for debugging.
        val realtimeSources = style.styleSources
            .map { it.id }
            .filter { id -> realtimeSourceKeywords.any { keyword -> id.contains(keyword, ignoreCase = true) } }
        Log.d(TAG, "Removing real-time sources: $realtimeSources")

        // Layers must be removed before their source can be removed.
        style.styleLayers
            .filter { layer -> realtimeSourceKeywords.any { kw -> layer.id.contains(kw, ignoreCase = true) } }
            .forEach { style.removeStyleLayer(it.id) }

        realtimeSources.forEach { style.removeStyleSource(it) }
    }

    // Keep for backward compat with toggleTraffic which still uses it during OFF mode.
    private fun removeTrafficSource(style: com.mapbox.maps.Style) = removeRealtimeSources(style)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun changeDestination(origin: List<Double>?, newDestination: List<Double>) {
        if (mapboxNavigation == null) return

        Log.d(TAG, "Alterando destino para: $newDestination")
        createRoute(origin, newDestination, null, true)

        sendEvent("destinationChanged", mapOf(
            "newDestinationLat" to newDestination[0],
            "newDestinationLng" to newDestination[1]
        ))
    }

    fun cancelNavigation() {
        pendingStartNavigation = false
        if (rerouteNetworkAcquired.getAndSet(false)) releaseNetwork()
        if (isTripSessionActive) {
            mapboxNavigation?.stopTripSession()
            isTripSessionActive = false
            sendEvent("tripSessionStateChanged", mapOf("active" to false))
        }
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

    private fun buildRouteOptions(origin: Point, destination: Point, waypoints: List<Point>?): RouteOptions {
        val coordinates = mutableListOf<Point>()
        coordinates.add(origin)
        waypoints?.let { coordinates.addAll(it) }
        coordinates.add(destination)

        val effectiveGeometry = when (dataSaverMode) {
            DataSaverMode.OFF -> routeGeometryPrecision
            DataSaverMode.BALANCED, DataSaverMode.AGGRESSIVE -> DirectionsCriteria.GEOMETRY_POLYLINE
        }
        val preferredOverview = when (dataSaverMode) {
            DataSaverMode.OFF -> routeOverview
            DataSaverMode.BALANCED, DataSaverMode.AGGRESSIVE -> {
                if (routeOverview == DirectionsCriteria.OVERVIEW_FULL) DirectionsCriteria.OVERVIEW_FULL
                else DirectionsCriteria.OVERVIEW_SIMPLIFIED
            }
        }
        val (effectiveOverview, effectiveAnnotations) = resolveOverviewAndAnnotations(preferredOverview)

        val optionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(coordinates)
            .profile(routeProfile)
            .language(routeLanguage)
            .voiceUnits(routeUnits)
            .geometries(effectiveGeometry)
            .overview(effectiveOverview)
            .annotationsList(effectiveAnnotations)
            // steps=true is always required: Navigation SDK v3 cannot parse routes without step
            // data, and applyDefaultNavigationOptions() sets banner_instructions=true which also
            // mandates steps. Setting steps=false causes "Leg is missing 'steps' field" failure.
            .steps(true)
            // voice_instructions=true is always required: even when using device TTS the
            // announcement text comes from the route response — without it announcement() is null.
            // Data saving comes from using device TTS instead of cloud TTS, not from omitting text.
            .voiceInstructions(true)
            .alternatives(allowAlternatives)
            .enableRefresh(enableRouteRefresh)

        if (isTruck) {
            val exclusions = mutableListOf<String>()
            exclusions.add(DirectionsCriteria.EXCLUDE_TOLL)
            exclusions.add(DirectionsCriteria.EXCLUDE_FERRY)
            exclusions.add(DirectionsCriteria.EXCLUDE_UNPAVED)
            exclusions.addAll(avoidList)

            optionsBuilder.excludeList(exclusions)
            maxHeight?.let { optionsBuilder.maxHeight(it) }
            maxWeight?.let { optionsBuilder.maxWeight(it) }
            maxWidth?.let { optionsBuilder.maxWidth(it) }
        }

        return optionsBuilder.build()
    }

    private fun resolveOverviewAndAnnotations(preferredOverview: String): Pair<String, List<String>> {
        val normalizedCustomAnnotations = routeAnnotations
            ?.map { it.lowercase(Locale.US).trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()

        val defaultAnnotations = when (dataSaverMode) {
            DataSaverMode.OFF -> listOf("duration", "distance", "speed", "congestion_numeric", "maxspeed")
            // congestion_numeric omitted: traffic layers are removed in these modes,
            // so the per-coordinate congestion data is downloaded but never rendered.
            DataSaverMode.BALANCED, DataSaverMode.AGGRESSIVE ->
                listOf("duration", "distance", "speed")
        }

        val hasCustomAnnotations = normalizedCustomAnnotations != null
        val mutableAnnotations = (normalizedCustomAnnotations ?: defaultAnnotations).toMutableList()
        var effectiveOverview = preferredOverview

        if (mutableAnnotations.contains("maxspeed") && preferredOverview != DirectionsCriteria.OVERVIEW_FULL) {
            if (hasCustomAnnotations) {
                effectiveOverview = DirectionsCriteria.OVERVIEW_FULL
            } else {
                mutableAnnotations.remove("maxspeed")
            }
        }

        return effectiveOverview to mutableAnnotations
    }

    private fun buildRouteSignature(origin: Point, destination: Point, waypoints: List<Point>?): String {
        val waypointKey = waypoints
            ?.joinToString("|") { "${it.latitude()},${it.longitude()}" }
            ?: ""
        return listOf(
            origin.latitude(),
            origin.longitude(),
            destination.latitude(),
            destination.longitude(),
            routeProfile,
            routeLanguage,
            routeUnits,
            allowAlternatives,
            enableRouteRefresh,
            isTruck,
            avoidList.joinToString(","),
            waypointKey,
        ).joinToString(";")
    }

    private fun shouldEmitOfflineProgress(region: String, percent: Double): Boolean {
        val lastPercent = lastOfflineProgressPercentByRegion[region] ?: -1.0
        val isComplete = percent >= 100.0
        val significantProgress = (percent - lastPercent) >= 1.0
        if (!isComplete && !significantProgress) {
            return false
        }

        val limiter = offlineProgressLimiterByRegion.getOrPut(region) { EventRateLimiter() }
        if (!isComplete && !limiter.shouldEmit(performancePolicy.offlineProgressEventMinIntervalMs)) {
            return false
        }

        lastOfflineProgressPercentByRegion[region] = percent
        return true
    }

    private fun isRouteInForbiddenZone(route: DirectionsRoute): Boolean {
        val geometry = route.geometry() ?: return false
        val routeLine = LineString.fromPolyline(geometry, 6)
        for (point in routeLine.coordinates()) {
            for (zone in forbiddenZones) {
                if (TurfJoins.inside(point, zone)) {
                    return true
                }
            }
        }
        return false
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>? = null) {
        // Prefer explicitly-passed routes (avoids race with routeLineApi async update).
        // Fall back to routeLineApi only when called from startNavigation with an existing route.
        val currentRoutes = routes ?: routeLineApi.getNavigationRoutes()
        if (currentRoutes.isNotEmpty()) {
            if (!isTripSessionActive) {
                mapboxNavigation?.startTripSession()
                isTripSessionActive = true
                sendEvent("tripSessionStateChanged", mapOf("active" to true))
            }
            mapboxNavigation?.setNavigationRoutes(currentRoutes)
            navigationCamera?.requestNavigationCameraToFollowing()
            sendEvent("navigationStarted", null)
        }
    }

    private fun sendEvent(type: String, data: Map<String, Any?>?) {
        if (dataSaverMode == DataSaverMode.AGGRESSIVE &&
            (type == "tripProgressUpdate" || type == "locationUpdate" || type == "maneuverUpdate")) {
            return
        }
        val payload = data?.toMutableMap() ?: mutableMapOf()
        payload["type"] = type
        val jsonPayload = JSONObject(payload).toString()
        activityPluginBinding?.activity?.runOnUiThread {
            try {
                eventSink?.success(jsonPayload)
            } catch (e: Exception) {
                if (dataSaverMode == DataSaverMode.OFF) Log.e(TAG, "Erro ao enviar evento para Flutter: $e")
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
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        networkCallback = null
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
        routeRequestGate.reset()
        locationEventLimiter.reset()
        tripProgressEventLimiter.reset()
        offlineProgressLimiterByRegion.clear()
        lastOfflineProgressPercentByRegion.clear()
        activityPluginBinding = null
        lifecycleHelper?.dispose()
        lifecycleHelper = null
        mapView?.setViewTreeLifecycleOwner(null)
        Log.d(TAG, "MapboxPlatformView com ID $viewId descartada e recursos liberados.")
    }

    // ── Region-bounds persistence ─────────────────────────────────────────────

    private fun saveRegionBounds(regionId: String, north: Double, east: Double, south: Double, west: Double) {
        context.getSharedPreferences(REGION_BOUNDS_PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(regionId, "$north,$east,$south,$west").apply()
        downloadedRegionBounds[regionId] = RegionBounds(north, east, south, west)
    }

    private fun removeRegionBounds(regionId: String) {
        context.getSharedPreferences(REGION_BOUNDS_PREFS, android.content.Context.MODE_PRIVATE)
            .edit().remove(regionId).apply()
        downloadedRegionBounds.remove(regionId)
    }

    private fun loadAllRegionBounds() {
        val prefs = context.getSharedPreferences(REGION_BOUNDS_PREFS, android.content.Context.MODE_PRIVATE)
        downloadedRegionBounds.clear()
        prefs.all.forEach { (key, value) ->
            val parts = (value as? String)?.split(",") ?: return@forEach
            if (parts.size == 4) {
                val n = parts[0].toDoubleOrNull() ?: return@forEach
                val e = parts[1].toDoubleOrNull() ?: return@forEach
                val s = parts[2].toDoubleOrNull() ?: return@forEach
                val w = parts[3].toDoubleOrNull() ?: return@forEach
                downloadedRegionBounds[key] = RegionBounds(n, e, s, w)
            }
        }
        Log.d(TAG, "Limites de regiões carregados: ${downloadedRegionBounds.size}")
    }

    // ── Location-based area detection ────────────────────────────────────────

    private fun checkAreaStatus(lat: Double, lng: Double) {
        // Only meaningful when regions exist and we have their bounds stored.
        if (!hasDownloadedRegions || downloadedRegionBounds.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastAreaCheckMs < AREA_CHECK_INTERVAL_MS) return
        lastAreaCheckMs = now

        val insideARegion = downloadedRegionBounds.values.any { it.contains(lat, lng) }
        if (!insideARegion && !isInUndownloadedArea) {
            enterUndownloadedArea()
        } else if (insideARegion && isInUndownloadedArea) {
            exitUndownloadedArea()
        }
    }

    private fun enterUndownloadedArea() {
        isInUndownloadedArea = true
        // Enable network so map tiles can be fetched, but cap the zoom so only
        // lightweight overview tiles are loaded (zoom 14 ≈ 10× less data than zoom 17).
        if (hasDownloadedRegions) {
            setMapboxNetwork(true)
            Log.d(TAG, "Fora de região baixada — rede activada (zoom cap $UNDOWNLOADED_AREA_MAX_ZOOM)")
        }
        // Block both the automatic camera AND manual gesture zoom so the user cannot
        // accidentally trigger high-zoom tile downloads in non-downloaded areas.
        _mapView?.mapboxMap?.setMaxZoom(UNDOWNLOADED_AREA_MAX_ZOOM)
        if (::viewportDataSource.isInitialized) {
            viewportDataSource.options.followingFrameOptions.maxZoom = UNDOWNLOADED_AREA_MAX_ZOOM
            viewportDataSource.options.overviewFrameOptions.maxZoom = UNDOWNLOADED_AREA_MAX_ZOOM - 2.0
            viewportDataSource.evaluate()
        }
        sendEvent("mapCoverageChanged", mapOf("inDownloadedRegion" to false, "maxZoom" to UNDOWNLOADED_AREA_MAX_ZOOM))
    }

    private fun exitUndownloadedArea() {
        isInUndownloadedArea = false
        // Restore full gesture zoom range before applying mode-specific viewport caps.
        _mapView?.mapboxMap?.setMaxZoom(22.0)
        // Only cut the network if no explicit operation is keeping it open.
        if (hasDownloadedRegions && networkRefCount.get() == 0) {
            setMapboxNetwork(false)
            Log.d(TAG, "De volta a região baixada — rede desactivada")
        }
        // Restore zoom limits for the active data-saver mode.
        applyMapDataSaverConfig()
        sendEvent("mapCoverageChanged", mapOf("inDownloadedRegion" to true))
    }

    // Bounding box of an explicitly downloaded region.
    private data class RegionBounds(
        val north: Double, val east: Double,
        val south: Double, val west: Double
    ) {
        fun contains(lat: Double, lng: Double) = lat in south..north && lng in west..east
    }

    companion object {
        // Singleton so all view instances (and recreations) share the same underlying store,
        // preventing duplicate downloads and ensuring MapboxMapsOptions.tileStore always
        // points to the same object across the app's lifetime.
        val tileStore: TileStore by lazy { TileStore.create() }

        // Maximum zoom used when rendering tiles in non-downloaded areas.
        // Zoom 14 covers ~5 km and uses ~10× fewer tiles than zoom 17 (navigation level).
        private const val UNDOWNLOADED_AREA_MAX_ZOOM = 14.0
        // Minimum interval between location-based region checks (avoids running on every GPS fix).
        private const val AREA_CHECK_INTERVAL_MS = 10_000L
        private const val REGION_BOUNDS_PREFS = "mapbox_nav_region_bounds"
    }
}
