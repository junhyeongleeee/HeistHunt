package com.heisthunt.shared.utils

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0 // 지구 반경 (미터)

    /**
     * 도(degree)를 라디안(radian)으로 변환
     */
    private fun Double.toRadians(): Double = this * PI / 180.0

    /**
     * 두 위치 간의 거리를 미터 단위로 계산 (Haversine formula)
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()

        val a = sin(dLat / 2).pow(2) +
                cos(lat1.toRadians()) *
                cos(lat2.toRadians()) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * 두 위치가 특정 거리(미터) 이내에 있는지 확인
     */
    fun isWithinRadius(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        radiusMeters: Double
    ): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= radiusMeters
    }
}
