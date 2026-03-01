package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    // Android Emulator에서는 10.0.2.2를 사용하여 호스트의 localhost에 접근
    // 실제 기기에서는 컴퓨터의 IP 주소 사용
    return "http://172.28.36.152:8080"
}
