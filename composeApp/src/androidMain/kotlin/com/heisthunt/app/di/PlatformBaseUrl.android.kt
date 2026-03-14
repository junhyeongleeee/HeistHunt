package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    // 에뮬레이터 감지: fingerprint/hardware/model로 판단
    val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK")
    return if (isEmulator) {
        "http://10.0.2.2:8080"
    } else {
        "http://172.28.36.59:8080"
    }
}
