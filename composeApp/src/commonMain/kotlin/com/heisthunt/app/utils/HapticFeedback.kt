package com.heisthunt.app.utils

expect class HapticFeedback {
    fun light()    // 가벼운 진동
    fun medium()   // 중간 진동
    fun heavy()    // 강한 진동
    fun success()  // 성공 패턴
    fun warning()  // 경고 패턴
}
