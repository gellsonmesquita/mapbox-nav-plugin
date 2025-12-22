package com.plugin.mapboxnav.domain.utils


import com.mapbox.geojson.Point

fun List<Double>.toPoint(): Point? {
    return if (this.size >= 2) {
        Point.fromLngLat(this[0], this[1])
    } else null
}

fun List<List<Double>>.toPoints(): List<Point> {
    return this.mapNotNull { it.toPoint() }
}