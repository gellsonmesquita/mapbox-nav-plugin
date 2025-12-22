package com.plugin.mapboxnav.infrastructure.channels

import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import com.plugin.mapboxnav.presentation.views.MapboxPlatformView
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


class MethodChannelHandler : MethodChannel.MethodCallHandler {

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

            "startNavigation" -> {
                handleStartNavigation(call, result, targetView)
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

        if (newDestination == null) {
            result.error("MISSING_ARG", "New destination is required", null)
            return
        }

        targetView.changeDestination(newDestination)
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