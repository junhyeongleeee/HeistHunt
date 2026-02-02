package com.heisthunt.app

import android.app.Application
import com.google.firebase.FirebaseApp

class HeistHuntApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase 초기화 (이미 google-services.json이 있으면 자동 초기화됨)
        // 명시적으로 초기화하려면 아래 주석 해제
        // FirebaseApp.initializeApp(this)
    }
}
