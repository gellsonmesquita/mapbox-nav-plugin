package com.plugin.mapboxnav


import android.content.Context
import androidx.lifecycle.Lifecycle
import com.mapbox.common.MapboxOptions
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class MapboxNavigationViewFactory(
    private val messenger: BinaryMessenger,
    private val currentActivityBinding: ActivityPluginBinding,
    private val lifecycleProvider: Lifecycle
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {


    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        MapboxOptions.accessToken = context.getString(R.string.mapbox_access_token)
        val mapboxView = MapboxPlatformView(
            context,
            messenger,
            MapboxnavPlugin.EVENT_CHANNEL_BASE_NAME,
            viewId,
            currentActivityBinding,
            lifecycleProvider
        )

        MapboxViewManager.registerView(viewId, mapboxView)

        return mapboxView
    }
}