package com.plugin.mapboxnav.infrastructure.channels

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.plugin.mapboxnav.core.config.MapboxConfig
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class MethodChannelHandler : MethodChannel.MethodCallHandler {

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
            return
        }

        val viewId = call.argument<Int>("viewId")
        val targetView = viewId?.let { MapboxViewManager.getView(it) }
        if (targetView == null) {
            result.viewNotFound()
            return
        }

        if (!targetView.handleMethodCall(call, result)) {
            result.notImplemented()
        }
    }

    private fun MethodChannel.Result.viewNotFound() {
        error(
            "VIEW_NOT_FOUND",
            "MapboxPlatformView not found. Use `${MapboxConfig.EVENT_CHANNEL_BASE_NAME}/<viewId>/methods` or pass a valid viewId.",
            null
        )
    }
}