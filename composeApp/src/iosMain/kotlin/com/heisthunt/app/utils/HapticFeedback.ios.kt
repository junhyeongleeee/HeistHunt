package com.heisthunt.app.utils

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

actual class HapticFeedback {
    actual fun light() {
        println("📳 [HapticFeedback] Triggering light vibration")
        val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
        generator.prepare()
        generator.impactOccurred()
    }

    actual fun medium() {
        println("📳 [HapticFeedback] Triggering medium vibration")
        val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
        generator.prepare()
        generator.impactOccurred()
    }

    actual fun heavy() {
        println("📳 [HapticFeedback] Triggering heavy vibration")
        val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
        generator.prepare()
        generator.impactOccurred()
    }

    actual fun success() {
        println("📳 [HapticFeedback] Triggering success vibration")
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    actual fun warning() {
        println("📳 [HapticFeedback] Triggering warning vibration")
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
    }
}
