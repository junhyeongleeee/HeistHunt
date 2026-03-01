package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    // iOS Simulator는 localhost를 직접 사용
    return "http://172.28.36.152:8080"
}
