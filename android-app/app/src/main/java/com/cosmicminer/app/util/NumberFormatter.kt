package com.cosmicminer.app.util

import java.util.Locale
import kotlin.math.floor

object NumberFormatter {
    private val suffixes = listOf("", "K", "M", "B", "T", "Qa", "Qi")

    fun format(value: Double, decimalsIfLarge: Int = 1): String {
        if (value < 0) return "0"
        if (value < 1_000.0) {
            return floor(value).toLong().toString()
        }

        var scaled = value
        var tier = 0
        while (scaled >= 1_000.0 && tier < suffixes.lastIndex) {
            scaled /= 1_000.0
            tier++
        }

        val formatted = if (scaled >= 100.0 || scaled % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", scaled)
        } else {
            String.format(Locale.US, "%.${decimalsIfLarge}f", scaled).trimEnd('0').trimEnd('.')
        }
        return "$formatted${suffixes[tier]}"
    }

    fun formatRate(value: Double): String {
        if (value < 10.0 && value > 0.0 && value % 1.0 != 0.0) {
            return String.format(Locale.US, "%.1f", value)
        }
        return format(value)
    }
}
