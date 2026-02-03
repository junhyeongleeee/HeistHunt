package com.heisthunt.app.utils

import kotlin.math.pow
import kotlin.math.round

/**
 * Format a Double to a string with specified decimal places
 */
fun Double.formatDecimal(decimals: Int): String {
    val multiplier = 10.0.pow(decimals)
    val rounded = round(this * multiplier) / multiplier

    // Convert to string and ensure decimal places
    val str = rounded.toString()
    val parts = str.split('.')

    return if (parts.size == 2) {
        val intPart = parts[0]
        val decPart = parts[1].padEnd(decimals, '0').take(decimals)
        "$intPart.$decPart"
    } else {
        val decPart = "0".repeat(decimals)
        "$str.$decPart"
    }
}
