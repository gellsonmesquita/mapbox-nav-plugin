package com.plugin.mapboxnav

import androidx.lifecycle.Lifecycle
import com.plugin.mapboxnav.core.config.MapboxConfig
import com.plugin.mapboxnav.core.utils.Logger
import com.plugin.mapboxnav.infrastructure.channels.MethodChannelHandler
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import com.plugin.mapboxnav.presentation.lifecycle.MapboxLifecycleObserver
import com.plugin.mapboxnav.presentation.views.MapboxNavigationViewFactory
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformViewRegistry


class MapboxnavPlugin : FlutterPlugin, ActivityAware {

  private var methodChannel: MethodChannel? = null
  private var mapboxNavigationViewFactory: MapboxNavigationViewFactory? = null
  private var lifecycle: Lifecycle? = null
  private val lifecycleObserver = MapboxLifecycleObserver()

  companion object {
    var binaryMessenger: BinaryMessenger? = null
    var platformViewRegistry: PlatformViewRegistry? = null
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Logger.d("Plugin attached to engine")

    binaryMessenger = binding.binaryMessenger
    platformViewRegistry = binding.platformViewRegistry

    methodChannel = MethodChannel(
      binding.binaryMessenger,
      MapboxConfig.METHOD_CHANNEL_NAME
    ).apply {
      setMethodCallHandler(MethodChannelHandler())
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Logger.d("Plugin detached from engine")

    methodChannel?.setMethodCallHandler(null)
    methodChannel = null
    MapboxViewManager.clearAllViews()

    binaryMessenger = null
    platformViewRegistry = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Logger.d("Plugin attached to activity")

    lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    lifecycle?.addObserver(lifecycleObserver)

    val messenger = binaryMessenger ?: return
    val registry = platformViewRegistry ?: return
    val currentLifecycle = lifecycle ?: return

    mapboxNavigationViewFactory = MapboxNavigationViewFactory(
      messenger,
      currentLifecycle
    )

    registry.registerViewFactory(
      MapboxConfig.VIEW_TYPE_ID,
      mapboxNavigationViewFactory!!
    )
  }

  override fun onDetachedFromActivityForConfigChanges() {
    Logger.d("Plugin detached from activity for config changes")
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    Logger.d("Plugin reattached to activity for config changes")
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
    Logger.d("Plugin detached from activity")

    lifecycle?.removeObserver(lifecycleObserver)
    lifecycle = null
  }
}