# mapbox_nav_plugin

A Flutter plugin that enables developers to integrate **Mapbox Navigation** (Turn-by-Turn) into their applications. This plugin bridges Flutter with Mapbox's powerful native navigation SDKs.

## Prerequisites

Before using this plugin, you must have a [Mapbox account](https://www.mapbox.com/) and:

1. A **Public Access Token**.
2. A **Secret Access Token** with the `DOWNLOADS:READ` scope enabled.

---

## Installation

Add `mapbox_nav_plugin` to your `pubspec.yaml`:

```yaml
dependencies:
  mapbox_nav_plugin: ^1.0.1

```

---

## Android Configuration

Mapbox requires specific native configurations to download the SDK and authenticate the map.

### 1. Configure Secret Token (gradle.properties)

In your Android project root (usually `android/gradle.properties`), add your **Secret Access Token**:

```properties
MAPBOX_DOWNLOADS_TOKEN=your_secret_private_key_here

```

### 2. Configure Public Token (XML Resource)

Create a new resource file at `android/app/src/main/res/values/mapbox_access_token.xml` and add your **Public Access Token**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">your_public_key_here</string>
</resources>

```

### 3. Update Permissions

Ensure your `android/app/src/main/AndroidManifest.xml` includes the necessary location permissions:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

```

---

## Usage

To start navigation, import the package and use the main controller (adjust the snippet below to match your plugin's specific entry point):

```dart
import 'package:mapbox_nav_plugin/mapbox_nav_plugin.dart';

// Basic implementation example
void startNavigation() {
  // Logic to trigger the Mapbox Navigation UI
}

```

> **Note:** For a complete implementation, check the [example directory](https://www.google.com/search?q=https://github.com/gellsonmesquita/mapbox-nav-plugin/tree/main/example).

---

## Features

* **Turn-by-Turn Navigation**: Real-time voice and visual instructions.
* **Mapbox Maps Integration**: High-quality map rendering.
* **Native Performance**: Direct communication with Mapbox Android/iOS SDKs.

## Contribution and Issues

If you find a bug or want to contribute, please visit the official repository:
[github.com/gellsonmesquita/mapbox-nav-plugin](https://github.com/gellsonmesquita/mapbox-nav-plugin)
