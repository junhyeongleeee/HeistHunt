package com.heisthunt.app.location

import com.heisthunt.shared.models.Location
import kotlinx.datetime.Clock

/**
 * Mock location service for testing location-based features
 * Only available in debug builds
 */
object MockLocationService {
    var isEnabled = false
        private set

    private var currentScenario: TestScenario = TestScenario.SEOUL_CITY_HALL

    fun enable() {
        isEnabled = true
        println("🧪 Mock Location Service ENABLED")
    }

    fun disable() {
        isEnabled = false
        println("🧪 Mock Location Service DISABLED")
    }

    fun setScenario(scenario: TestScenario) {
        currentScenario = scenario
        println("🧪 Mock Location Scenario: ${scenario.displayName}")
    }

    fun setCustomLocation(latitude: Double, longitude: Double) {
        currentScenario = TestScenario.Custom(latitude, longitude)
        println("🧪 Mock Location Custom: ($latitude, $longitude)")
    }

    fun getCurrentLocation(): Location {
        return when (val scenario = currentScenario) {
            is TestScenario.SEOUL_CITY_HALL -> Location(
                latitude = 37.5665,
                longitude = 126.9780,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
            is TestScenario.GANGNAM_STATION -> Location(
                latitude = 37.4979,
                longitude = 127.0276,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
            is TestScenario.HONGDAE -> Location(
                latitude = 37.5563,
                longitude = 126.9236,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
            is TestScenario.NAMSAN_TOWER -> Location(
                latitude = 37.5512,
                longitude = 126.9882,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
            is TestScenario.COEX -> Location(
                latitude = 37.5125,
                longitude = 127.0594,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
            is TestScenario.Custom -> Location(
                latitude = scenario.latitude,
                longitude = scenario.longitude,
                accuracy = 5f,
                timestamp = Clock.System.now()
            )
        }
    }

    sealed class TestScenario {
        object SEOUL_CITY_HALL : TestScenario()
        object GANGNAM_STATION : TestScenario()
        object HONGDAE : TestScenario()
        object NAMSAN_TOWER : TestScenario()
        object COEX : TestScenario()
        data class Custom(val latitude: Double, val longitude: Double) : TestScenario()

        val displayName: String
            get() = when (this) {
                SEOUL_CITY_HALL -> "서울시청 (Seoul City Hall)"
                GANGNAM_STATION -> "강남역 (Gangnam Station)"
                HONGDAE -> "홍대입구 (Hongdae)"
                NAMSAN_TOWER -> "남산타워 (Namsan Tower)"
                COEX -> "코엑스 (COEX)"
                is Custom -> "Custom ($latitude, $longitude)"
            }

        companion object {
            fun values(): List<TestScenario> = listOf(
                SEOUL_CITY_HALL,
                GANGNAM_STATION,
                HONGDAE,
                NAMSAN_TOWER,
                COEX
            )
        }
    }
}
