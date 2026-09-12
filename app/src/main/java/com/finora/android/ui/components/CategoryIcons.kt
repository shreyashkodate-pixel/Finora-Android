package com.finora.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {

    val availableIcons = listOf(
        "restaurant" to "Dining",
        "directions_car" to "Transport",
        "home" to "Housing",
        "shopping_bag" to "Shopping",
        "receipt_long" to "Bills",
        "movie" to "Entertainment",
        "health_and_safety" to "Health",
        "school" to "Education",
        "laptop_mac" to "Work",
        "fitness_center" to "Fitness",
        "flight" to "Travel",
        "more_horiz" to "Other"
    )

    val availableColors = listOf(
        "#046B5E",
        "#004D40",
        "#203037",
        "#29695B",
        "#36464E",
        "#0F6F62",
        "#BA1A1A",
        "#00342B",
        "#707975",
        "#1E88E5",
        "#FB8C00",
        "#8E24AA"
    )

    fun getIcon(iconName: String, categoryName: String = ""): ImageVector {
        val mapped = when (iconName.lowercase()) {
            "restaurant", "fastfood" -> Icons.Default.Fastfood
            "directions_car", "transport", "transit" -> Icons.Default.DirectionsCar
            "home", "housing" -> Icons.Default.Home
            "shopping_bag", "shopping_cart", "shopping" -> Icons.Default.ShoppingBag
            "receipt_long", "receipt", "bills" -> Icons.Default.Receipt
            "movie", "entertainment", "theater_comedy" -> Icons.Default.Movie
            "school", "education" -> Icons.Default.School
            "health_and_safety", "health", "vital_signs" -> Icons.Default.LocalHospital
            "laptop_mac", "computer", "freelance" -> Icons.Default.Computer
            "fitness_center", "gym" -> Icons.Default.FitnessCenter
            "flight", "travel" -> Icons.Default.Flight
            else -> null
        }
        if (mapped != null) return mapped

        val nameLower = categoryName.lowercase()
        return when {
            nameLower.contains("food") || nameLower.contains("din") || nameLower.contains("restaurant") -> Icons.Default.Fastfood
            nameLower.contains("car") || nameLower.contains("transport") || nameLower.contains("transit") || nameLower.contains("travel") -> Icons.Default.DirectionsCar
            nameLower.contains("home") || nameLower.contains("rent") || nameLower.contains("house") -> Icons.Default.Home
            nameLower.contains("shop") || nameLower.contains("grocer") -> Icons.Default.ShoppingBag
            nameLower.contains("bill") || nameLower.contains("util") || nameLower.contains("receipt") -> Icons.Default.Receipt
            nameLower.contains("entertain") || nameLower.contains("movie") || nameLower.contains("cinema") -> Icons.Default.Movie
            nameLower.contains("health") || nameLower.contains("medic") || nameLower.contains("hospital") -> Icons.Default.LocalHospital
            nameLower.contains("school") || nameLower.contains("edu") || nameLower.contains("study") -> Icons.Default.School
            nameLower.contains("gym") || nameLower.contains("fit") -> Icons.Default.FitnessCenter
            nameLower.contains("work") || nameLower.contains("laptop") -> Icons.Default.Computer
            else -> Icons.Default.MoreHoriz
        }
    }

    fun parseColorHex(hex: String, fallback: Color = Color(0xFF046B5E)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(colorLong or 0x00000000FF000000L)
            } else {
                Color(colorLong)
            }
        } catch (_: Exception) {
            fallback
        }
    }
}

