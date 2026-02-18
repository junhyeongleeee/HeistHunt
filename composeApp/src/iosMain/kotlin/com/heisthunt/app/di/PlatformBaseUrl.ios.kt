package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    // iOS Simulator는 localhost를 직접 사용
    return "http://192.168.219.104:8080"
}
