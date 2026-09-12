package com.finora.android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "fastfood" -> Icons.Default.Fastfood
            "directions_car", "transport", "transit" -> Icons.Default.DirectionsCar
            "home", "housing" -> Icons.Default.Home
            "shopping_bag", "shopping_cart", "shopping" -> Icons.Default.ShoppingBag
            "receipt_long", "receipt", "bills" -> Icons.Default.Receipt
            "movie", "entertainment" -> Icons.Default.Movie
            "school", "education" -> Icons.Default.School
            else -> Icons.Default.MoreHoriz
        }
    }
}
