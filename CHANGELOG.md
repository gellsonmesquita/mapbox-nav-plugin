## 1.1.0

### 🚀 New Features

* **Full Offline Map Support**: Integrated `TileStore` and `OfflineManager` to allow downloading entire provinces or regions (e.g., Luanda Metropolitan Area).
* **Smart Route Caching**: Automatic "Route Corridor" downloading upon path generation, ensuring seamless navigation even in dead zones.
* **Persistent Storage Management**: Configured a **5 GB disk quota** for robust, long-term map tile caching.
* **Data Saving Mode**:
* Optimized API requests by disabling redundant traffic refreshes and incident reporting.
* Implemented **Local Glyph Rasterization** to significantly reduce mobile data usage by rendering street names locally.


* **Traffic Visibility Control**: Added a toggle to enable or disable the real-time traffic layer on demand.

### 🛠️ Technical Improvements

* **Mapbox SDK v11 (v3) Migration**: Upgraded to the latest Navigation SDK for improved performance and modern API support.
* **Enhanced Communication Bridge**: Refined `MethodChannel` and `EventChannel` logic for real-time offline download progress tracking.
* **Adaptive Viewport logic**: Improved camera padding for seamless transitions between Portrait and Landscape orientations.

### 🐞 Bug Fixes

* Fine-tuned location permissions for Android 12+ (S) compatibility.
* Fixed an issue where the `NavigationCamera` would jump abruptly during the initial positioning.


