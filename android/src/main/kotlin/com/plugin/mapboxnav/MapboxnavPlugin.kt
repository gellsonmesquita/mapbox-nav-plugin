package com.plugin.mapboxnav

import android.util.Log
import androidx.lifecycle.Lifecycle
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformViewRegistry

class MapboxnavPlugin: FlutterPlugin, ActivityAware {

  private lateinit var methodChannel: MethodChannel
  private lateinit var mapboxNavigationViewFactory: MapboxNavigationViewFactory
  private var activityPluginBinding: ActivityPluginBinding? = null
  private var lifecycleObserver: MapboxLifecycleObserver? = null
  private var lifecycle: Lifecycle? = null
  companion object {
    const val VIEW_TYPE_ID = "mapView"
    const val METHOD_CHANNEL_NAME = "com.app.mapbox_nav_plugin/method_channel"
    const val EVENT_CHANNEL_BASE_NAME = "com.app.mapbox_nav_plugin/event_channel"
    var binaryMessenger: BinaryMessenger? = null
    var platformViewRegistry: PlatformViewRegistry? = null
    var mapboxAccessToken: String? = null
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val messenger = flutterPluginBinding.binaryMessenger
    binaryMessenger = messenger
    platformViewRegistry = flutterPluginBinding.platformViewRegistry
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
    methodChannel.setMethodCallHandler { call, result ->
      val viewId = call.argument<Int>("viewId")
      val targetView = if (viewId != null) MapboxViewManager.getView(viewId) else null

      when (call.method) {
        "createRoute" -> {
          if (targetView != null) {
            val origin = call.argument<List<Double>>("origin")!!
            val destination = call.argument<List<Double>>("destination")!!
            val waypoints = call.argument<List<List<Double>>>("waypoints")
            targetView.createRoute(origin, destination, waypoints)
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "startNavigation" -> {
          if (targetView != null) {
            val origin = call.argument<List<Double>>("origin")
            val destination = call.argument<List<Double>>("destination")!!
            val waypoints = call.argument<List<List<Double>>>("waypoints")
            targetView.startNavigation(origin, destination, waypoints)
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "changeDestination" -> {
          if (targetView != null) {
            val newDestination = call.argument<List<Double>>("newDestination")!!
            targetView.changeDestination(newDestination)
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "cancelNavigation" -> {
          if (targetView != null) {
            targetView.cancelNavigation()
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "finishNavigation" -> {
          if (targetView != null) {
            targetView.finishNavigation()
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "stopNavigation" -> {
          if (targetView != null) {
            targetView.stopNavigation()
            result.success(true)
          } else {
            result.error("VIEW_NOT_FOUND", "MapboxPlatformView com ID $viewId não encontrada.", null)
          }
        }
        "getPlatformVersion" -> {
          result.success("Android ${android.os.Build.VERSION.RELEASE}")
        }
        else -> result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel.setMethodCallHandler(null)
    MapboxViewManager.clearAllViews()
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.activityPluginBinding = binding
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    if (platformViewRegistry != null && binaryMessenger != null && lifecycle != null) {
      platformViewRegistry?.registerViewFactory(
        VIEW_TYPE_ID,
        MapboxNavigationViewFactory(binaryMessenger!!, binding, lifecycle!!)
      )
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activityPluginBinding = null
    lifecycle = null
    Log.d("MapboxNavPlugin", "Plugin desanexado da Activity por mudança de config.")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    this.activityPluginBinding = binding
    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    Log.d("MapboxNavPlugin", "Plugin reanexado à Activity por mudança de config.")
  }

  override fun onDetachedFromActivity() {
    lifecycleObserver = null
    lifecycle = null
    activityPluginBinding = null
    Log.d("MapboxNavPlugin", "Plugin desanexado da Activity. Observador de ciclo de vida removido.")
  }
}
