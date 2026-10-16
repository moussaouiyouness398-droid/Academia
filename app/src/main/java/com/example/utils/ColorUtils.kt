package com.example.utils

import androidx.compose.ui.graphics.Color

object ColorUtils {
    /**
     * Safely parse a hex color string to Compose Color.
     * Prevents IllegalArgumentException crashes if the hex is invalid or malformed.
     */
    fun parseColorSafe(colorHex: String?, fallback: Color = Color(0xFF6750A4)): Color {
        if (colorHex.isNullOrBlank()) return fallback
        return try {
            val formatted = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
            Color(android.graphics.Color.parseColor(formatted))
        } catch (e: Exception) {
            fallback
        }
    }
}
