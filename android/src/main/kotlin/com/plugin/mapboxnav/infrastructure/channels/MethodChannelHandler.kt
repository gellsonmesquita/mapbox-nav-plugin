package com.plugin.mapboxnav.infrastructure.channels

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import com.plugin.mapboxnav.presentation.views.MapboxPlatformView
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class MethodChannelHandler : MethodChannel.MethodCallHandler {

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val viewId = call.argument<Int>("viewId")
        val targetView = viewId?.let { MapboxViewManager.getView(it) }

        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "createRoute" -> {
                handleCreateRoute(call, result, targetView)
            }

            "downloadOfflineArea" -> {
                val args = call.arguments as Map<*, *>
                val region = args["region"] as String
                val north = args["north"] as Double
                val east = args["east"] as Double
                val south = args["south"] as Double
                val west = args["west"] as Double
                targetView?.downloadRegionOffline(region,north, east, south, west)
                result.success(null)
            }
            "startNavigation" -> {
                handleStartNavigation(call, result, targetView)
            }
            "toggleTraffic" -> {
                val show = call.argument<Boolean>("show") ?: true
                targetView?.toggleTraffic(show)
                result.success(null)
            }
            "changeDestination" -> {
                handleChangeDestination(call, result, targetView)
            }
            "cancelNavigation" -> {
                handleSimpleAction(targetView, result) { it.cancelNavigation() }
            }

            "finishNavigation" -> {
                handleSimpleAction(targetView, result) { it.finishNavigation() }
            }

            "stopNavigation" -> {
                handleSimpleAction(targetView, result) { it.stopNavigation() }
            }

            else -> {
                result.notImplemented()
            }
        }
    }


    private fun handleCreateRoute(
        call: MethodCall,
        result: MethodChannel.Result,
        targetView: MapboxPlatformView?
    ) {
        if (targetView == null) {
            result.viewNotFound()
            return
        }

        val origin = call.argument<List<Double>>("origin")
        val destination = call.argument<List<Double>>("destination")
        val waypoints = call.argument<List<List<Double>>>("waypoints")

        if (origin == null || destination == null) {
            result.error("MISSING_ARG", "Origin and destination are required", null)
            return
        }

        targetView.createRoute(origin, destination, waypoints)
        result.success(true)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleStartNavigation(
        call: MethodCall,
        result: MethodChannel.Result,
        targetView: MapboxPlatformView?
    ) {
        if (targetView == null) {
            result.viewNotFound()
            return
        }

        val origin = call.argument<List<Double>>("origin")
        val destination = call.argument<List<Double>>("destination")
        val waypoints = call.argument<List<List<Double>>>("waypoints")

        if (destination == null) {
            result.error("MISSING_ARG", "Destination is required", null)
            return
        }

        targetView.startNavigation(origin, destination, waypoints)
        result.success(true)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleChangeDestination(
        call: MethodCall,
        result: MethodChannel.Result,
        targetView: MapboxPlatformView?
    ) {
        if (targetView == null) {
            result.viewNotFound()
            return
        }

        val newDestination = call.argument<List<Double>>("newDestination")
        val origin = call.argument<List<Double>>("origin")

        if (newDestination == null) {
            result.error("MISSING_ARG", "New destination is required", null)
            return
        }

        targetView.changeDestination(origin, newDestination)
        result.success(true)
    }

    private inline fun handleSimpleAction(
        targetView: MapboxPlatformView?,
        result: MethodChannel.Result,
        action: (MapboxPlatformView) -> Unit
    ) {
        if (targetView == null) {
            result.viewNotFound()
            return
        }
        action(targetView)
        result.success(true)
    }

    private fun MethodChannel.Result.viewNotFound() {
        error("VIEW_NOT_FOUND", "MapboxPlatformView not found", null)
    }
}