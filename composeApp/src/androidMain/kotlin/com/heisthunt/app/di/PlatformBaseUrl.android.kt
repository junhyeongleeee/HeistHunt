package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK")
    return if (isEmulator) {
        "http://10.0.2.2:8080"
    } else {
        "http://192.168.1.145:8080"
    }
}
