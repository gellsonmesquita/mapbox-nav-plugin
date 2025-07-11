package com.app.mapbox_nav_plugin

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.app.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.platform.PlatformView
import org.json.JSONObject
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapsResourceOptions
import io.flutter.embedding.android.FlutterActivity

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

    private lateinit var eventChannel: EventChannel
    private var eventSink: EventSink? = null

    private lateinit var containerView: View
    private var _mapView: MapView? = null
    val mapView: MapView? get() = _mapView

    private var mapboxNavigation: MapboxNavigation? = null
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var replayProgressObserver: ReplayProgressObserver
    private val navigationLocationProvider = NavigationLocationProvider()
    private val replayRouteMapper = ReplayRouteMapper()
    private var currentDirectionsRoute: DirectionsRoute? = null

    private var lifecycleHelper: LifecycleHelper? = null

    // Observers
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            val primaryRoute = routeUpdateResult.navigationRoutes.first()
            currentDirectionsRoute = primaryRoute.directionsRoute // Atualiza a rota atual
            Log.d(TAG, "Rota atualizada. Distância da rota: ${primaryRoute.directionsRoute.distance()}")

            routeLineApi.setNavigationRoutes(routeUpdateResult.navigationRoutes) { value ->
                _mapView?.mapboxMap?.style?.apply { routeLineView.renderRouteDrawData(this, value) }
            }

            viewportDataSource.onRouteChanged(primaryRoute) // Usa NavigationRoute
            viewportDataSource.evaluate()
            navigationCamera.requestNavigationCameraToOverview()
            sendEvent("routeCreated", mapOf(
                "routeId" to primaryRoute.directionsRoute.hashCode().toString(),
                "routeCount" to routeUpdateResult.navigationRoutes.size
            ))
        } else {
            sendEvent("routeCreated", mapOf("routeCount" to 0))
            Log.d(TAG, "Nenhuma rota encontrada no RouteObserver.")
        }
    }

    private val locationObserver = object : LocationObserver {

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            sendEvent("locationUpdate", mapOf(
                "latitude" to enhancedLocation.latitude,
                "longitude" to enhancedLocation.longitude,
                "bearing" to enhancedLocation.bearing,
                "accuracy" to  enhancedLocation.bearingAccuracy,
                "speed" to enhancedLocation.speed
            ))

            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()
            navigationCamera.requestNavigationCameraToFollowing()
        }

        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {
            TODO("Not yet implemented")
        }
    }

    private var activityLifecycleObserver: DefaultLifecycleObserver? = null // Renomeado para evitar conflito

    init {
        eventChannel = EventChannel(messenger, "$eventChannelBaseName/$viewId")
        eventChannel.setStreamHandler(this)
        val currentActivity = activityPluginBinding?.activity
        // Instancie o MapView
        _mapView = MapView(context)
        // Crie o FrameLayout que será a view raiz do PlatformView
        containerView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Adicione o MapView ao FrameLayout
            addView(_mapView)
        }

         //Configuração inicial do MapView
        _mapView?.mapboxMap?.loadStyle(com.mapbox.maps.Style.MAPBOX_STREETS) { style ->
            _mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(-0.1278, 51.5074))
                    .zoom(12.0)
                    .build()
            )

            _mapView?.location?.apply {
                setLocationProvider(navigationLocationProvider)
                locationPuck = createDefault2DPuck()
                enabled = true
            }
            _mapView?.gestures?.addOnMapClickListener { point ->
                Log.d(TAG, "Map clicked at: ${point.latitude()}, ${point.longitude()}")
                sendEvent("mapClicked", mapOf("latitude" to point.latitude(), "longitude" to point.longitude()))
                true
            }
            sendEvent("pluginInitialized", mapOf("viewId" to viewId))

            initializeNavigationComponents()
        } ?: run {
            Log.e(TAG, "MapView não foi inicializado corretamente ou falhou ao carregar o estilo.")
            sendEvent("error", mapOf("message" to "MapView failed to initialize or load style."))
        }

    }

    override fun onFlutterViewAttached(flutterView: View) {
        super.onFlutterViewAttached(flutterView)
        val context = flutterView.context
        val shouldDestroyOnDestroy = when (context is FlutterActivity) {
            true -> context.shouldDestroyEngineWithHost()
            false -> true
        }
        lifecycleHelper = LifecycleHelper(lifecycleProvider, shouldDestroyOnDestroy)
        mapView?.setViewTreeLifecycleOwner(lifecycleHelper)
        MapboxNavigationApp.setup(NavigationOptions.Builder(context).build())
        MapboxNavigationApp.attach(lifecycleHelper!!)
        mapboxNavigation = MapboxNavigationApp.current()
    }

    override fun onFlutterViewDetached() {
        super.onFlutterViewDetached()
        lifecycleHelper?.dispose()
        lifecycleHelper = null
        mapView?.setViewTreeLifecycleOwner(null)
    }


    private fun initializeNavigationComponents() {
        // Inicializa ViewportDataSource e NavigationCamera
        viewportDataSource = MapboxNavigationViewportDataSource(_mapView!!.mapboxMap)
        val pixelDensity = context.resources.displayMetrics.density
        viewportDataSource.followingPadding = EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
        navigationCamera = NavigationCamera(_mapView!!.mapboxMap, _mapView!!.camera, viewportDataSource)

        // Inicializa RouteLineApi e View
        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(MapboxRouteLineViewOptions.Builder(context).build())

        // Registra observadores
        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)

        // Para simulação de rota (opcional, igual ao exemplo MainActivity)
        replayProgressObserver = ReplayProgressObserver(mapboxNavigation!!.mapboxReplayer)
        mapboxNavigation?.registerRouteProgressObserver(replayProgressObserver)

        Log.d(TAG, "Componentes de navegação inicializados.")
    }

    @SuppressLint("MissingPermission")
    fun createRoute(origin: List<Double>, destination: List<Double>, waypoints: List<List<Double>>?) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para criar rota.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for route creation."))
            return
        }

        val originPoint = Point.fromLngLat(origin[1], origin[0])
        val destinationPoint = Point.fromLngLat(destination[1], destination[0])

        val routeOptionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .steps(true)
            .language("pt-BR")
            .coordinatesList(listOf(originPoint, destinationPoint))
            .profile("mapbox/driving-traffic")

        mapboxNavigation?.requestRoutes(
            routeOptionsBuilder.build(),
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
                        mapboxNavigation?.setNavigationRoutes(routes) // Define a rota no MapboxNavigation
                        // A atualização da UI (linhas da rota, câmera) é feita via routesObserver
                        // A simulação pode ser iniciada aqui ou em startNavigation
                        currentDirectionsRoute = routes.first().directionsRoute // Armazena para uso posterior
                    } else {
                        Log.d(TAG, "Nenhuma rota encontrada.")
                        sendEvent("routeCreated", mapOf("routeCount" to 0))
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startNavigation(origin: List<Double>?, destination: List<Double>, waypoints: List<List<Double>>?) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para iniciar a navegação.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for starting navigation."))
            return
        }

        if (currentDirectionsRoute == null) {
            // Se a rota não foi criada, tente criá-la. A navegação começará após o callback onRoutesReady.
            Log.d(TAG, "Rota não existe, tentando criar antes de iniciar a navegação.")
            createRoute(origin ?: emptyList(), destination, waypoints)
            return
        }

        // Inicia a sessão de viagem (e a simulação, se configurada)
        mapboxNavigation?.startTripSession()
        sendEvent("navigationStarted", null)
        Log.d(TAG, "Navegação iniciada.")

        // Inicia simulação de movimento do usuário se uma rota foi carregada
        currentDirectionsRoute?.let { route ->
            val replayData = replayRouteMapper.mapDirectionsRouteGeometry(route)
            mapboxNavigation?.mapboxReplayer?.pushEvents(replayData)
            mapboxNavigation?.mapboxReplayer?.seekTo(replayData[0])
            mapboxNavigation?.mapboxReplayer?.play()
            Log.d(TAG, "Simulação de rota iniciada.")
        } ?: Log.w(TAG, "Não há rota para iniciar a simulação.")
    }

    fun changeDestination(newDestination: List<Double>) {
        if (mapboxNavigation == null) {
            Log.e(TAG, "MapboxNavigation não está inicializado para mudar destino.")
            sendEvent("error", mapOf("message" to "MapboxNavigation not initialized for changing destination."))
            return
        }

        val newDestinationPoint = Point.fromLngLat(newDestination[1], newDestination[0])
        val currentLocation = navigationLocationProvider.lastLocation // Pega a última localização do provedor

        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
            createRoute(listOf(originPoint.latitude(), originPoint.longitude()), newDestination, null) // Recria a rota
            sendEvent("destinationChanged", mapOf("newDestinationLat" to newDestination[0], "newDestinationLng" to newDestination[1]))
            Log.d(TAG, "Destino alterado, recalculando rota.")
        } else {
            Log.e(TAG, "Não é possível mudar o destino, localização atual desconhecida.")
            sendEvent("error", mapOf("message" to "Current location unknown to change destination."))
        }
    }

    fun cancelNavigation() {
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.setNavigationRoutes(emptyList()) // Limpa as rotas
        _mapView?.mapboxMap?.style?.let { style -> // Adicionado null check seguro aqui
            routeLineView.cancel()
        }
        currentDirectionsRoute = null
        mapboxNavigation?.mapboxReplayer?.clearEvents() // Limpa eventos de simulação
        mapboxNavigation?.mapboxReplayer?.stop() // Para o replayer
        sendEvent("navigationCancelled", null)
        Log.d(TAG, "Navegação cancelada.")
    }

    fun finishNavigation() {
        // Para a sessão de viagem e limpa a rota
        cancelNavigation() // Reutiliza a lógica de cancelamento para finalizar
        sendEvent("navigationFinished", null)
        Log.d(TAG, "Navegação finalizada.")
    }

    fun stopNavigation() {
        cancelNavigation()
    }

    /**
     * Envia um evento para o lado Flutter via EventChannel.
     * @param type O tipo de evento (ex: "routeProgressUpdate", "navigationStarted").
     * @param data Dados adicionais do evento.
     */
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

    // --- Implementação do EventChannel.StreamHandler ---
    override fun onListen(arguments: Any?, events: EventSink) {
        this.eventSink = events
        Log.d(TAG, "EventChannel onListen para viewId $viewId")
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
        Log.d(TAG, "EventChannel onCancel para viewId $viewId")
    }

    // --- Implementação do PlatformView (Ciclo de Vida da View Nativa) ---
    override fun getView(): View {
        return containerView
    }

    @SuppressLint("MissingPermission") // Anotação necessária se estiver usando permissões de localização
    override fun dispose() {
        Log.d(TAG, "Disposing MapboxPlatformView com ID $viewId")
        MapboxViewManager.unregisterView(viewId)
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.setNavigationRoutes(emptyList())
        if (_mapView != null && _mapView?.mapboxMap?.style != null) {
            routeLineApi.cancel()
        }
        _mapView?.gestures?.removeOnMapClickListener { true }
        mapboxNavigation?.mapboxReplayer?.clearEvents()
        mapboxNavigation?.mapboxReplayer?.stop()
        mapboxNavigation = null
        _mapView?.onDestroy()
        _mapView = null
        eventChannel.setStreamHandler(null)
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