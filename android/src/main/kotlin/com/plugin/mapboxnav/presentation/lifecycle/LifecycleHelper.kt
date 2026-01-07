package com.plugin.mapboxnav.presentation.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


internal class LifecycleHelper(
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