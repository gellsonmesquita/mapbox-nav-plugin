package com.plugin.mapboxnav.data.managers

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.plugin.mapboxnav.domain.models.Point

class LocationManager(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationFound: (Point?) -> Unit) {
        val priority = Priority.PRIORITY_HIGH_ACCURACY

        fusedLocationClient.getCurrentLocation(priority, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFound(Point(location.latitude, location.longitude))
                } else {
                    onLocationFound(null)
                }
            }
            .addOnFailureListener {
                onLocationFound(null)
            }
    }
}