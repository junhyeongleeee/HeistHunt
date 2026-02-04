package com.heisthunt.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.storage.SecureStorage

class HeistHuntApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize SecureStorage
        val secureStorage = SecureStorage(this)
        AppModule.initialize(secureStorage)

        // Firebase 초기화 (이미 google-services.json이 있으면 자동 초기화됨)
        // 명시적으로 초기화하려면 아래 주석 해제
        // FirebaseApp.initializeApp(this)
    }
}
