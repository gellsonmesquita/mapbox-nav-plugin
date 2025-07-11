package com.app.mapbox_nav_plugin

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.mapbox.common.MapboxOptions
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

/**
 * Fábrica para criar instâncias de MapboxPlatformView.
 * Registra a view recém-criada no MapboxViewManager.
 */
class MapboxNavigationViewFactory(
    private val messenger: BinaryMessenger,
    private val currentActivityBinding: ActivityPluginBinding,
    private val lifecycleProvider: Lifecycle
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {


    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        MapboxOptions.accessToken = context.getString(R.string.mapbox_access_token)
        // Cria a instância de MapboxPlatformView
        val mapboxView = MapboxPlatformView(
            context,
            messenger,
            MapboxNavPlugin.EVENT_CHANNEL_BASE_NAME,
            viewId,
            currentActivityBinding,
            lifecycleProvider
        )

        // Registra a view recém-criada no MapboxViewManager
        MapboxViewManager.registerView(viewId, mapboxView)

        return mapboxView
    }
}