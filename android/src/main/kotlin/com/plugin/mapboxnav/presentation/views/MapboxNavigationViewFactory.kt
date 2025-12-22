package com.plugin.mapboxnav.presentation.views


import android.content.Context
import androidx.lifecycle.Lifecycle
import com.mapbox.common.MapboxOptions
import com.plugin.mapboxnav.core.config.MapboxConfig
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import  com.plugin.mapboxnav.R
import com.plugin.mapboxnav.infrastructure.registry.MapboxViewManager


class MapboxNavigationViewFactory(
    private val messenger: BinaryMessenger,
    private val lifecycleProvider: Lifecycle
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        // Set Mapbox access token
        MapboxOptions.accessToken = context.getString(R.string.mapbox_access_token)

        val mapboxView = MapboxPlatformView(
            context,
            messenger,
            MapboxConfig.EVENT_CHANNEL_BASE_NAME,
            viewId,
            lifecycleProvider
        )

        MapboxViewManager.registerView(viewId, mapboxView)

        return mapboxView
    }
}