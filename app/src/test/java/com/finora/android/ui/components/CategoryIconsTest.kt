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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryIconsTest {

    @Test
    fun getIcon_resolvesCanonicalIconNames() {
        assertEquals(Icons.Default.Fastfood, CategoryIcons.getIcon("restaurant"))
        assertEquals(Icons.Default.DirectionsCar, CategoryIcons.getIcon("directions_car"))
        assertEquals(Icons.Default.Home, CategoryIcons.getIcon("home"))
        assertEquals(Icons.Default.ShoppingBag, CategoryIcons.getIcon("shopping_bag"))
        assertEquals(Icons.Default.Receipt, CategoryIcons.getIcon("receipt_long"))
        assertEquals(Icons.Default.Movie, CategoryIcons.getIcon("movie"))
        assertEquals(Icons.Default.LocalHospital, CategoryIcons.getIcon("health_and_safety"))
        assertEquals(Icons.Default.School, CategoryIcons.getIcon("school"))
        assertEquals(Icons.Default.Computer, CategoryIcons.getIcon("laptop_mac"))
        assertEquals(Icons.Default.FitnessCenter, CategoryIcons.getIcon("fitness_center"))
        assertEquals(Icons.Default.Flight, CategoryIcons.getIcon("flight"))
    }

    @Test
    fun getIcon_resolvesFallbackByCategoryName() {
        assertEquals(Icons.Default.Fastfood, CategoryIcons.getIcon("unknown", "Dining Out"))
        assertEquals(Icons.Default.DirectionsCar, CategoryIcons.getIcon("unknown", "Transport Pass"))
        assertEquals(Icons.Default.Home, CategoryIcons.getIcon("unknown", "Apartment Rent"))
        assertEquals(Icons.Default.ShoppingBag, CategoryIcons.getIcon("unknown", "Groceries"))
        assertEquals(Icons.Default.Receipt, CategoryIcons.getIcon("unknown", "Utility Bills"))
        assertEquals(Icons.Default.Movie, CategoryIcons.getIcon("unknown", "Cinema & Movies"))
        assertEquals(Icons.Default.LocalHospital, CategoryIcons.getIcon("unknown", "Medical Checkup"))
        assertEquals(Icons.Default.School, CategoryIcons.getIcon("unknown", "Education Fees"))
        assertEquals(Icons.Default.FitnessCenter, CategoryIcons.getIcon("unknown", "Gym Membership"))
        assertEquals(Icons.Default.Computer, CategoryIcons.getIcon("unknown", "Laptop Repairs"))
    }

    @Test
    fun getIcon_returnsMoreHorizForUnknown() {
        assertEquals(Icons.Default.MoreHoriz, CategoryIcons.getIcon("unknown", "Miscellaneous"))
    }

    @Test
    fun parseColorHex_parsesValidHexValues() {
        val color = CategoryIcons.parseColorHex("#046B5E")
        assertNotEquals(Color.Unspecified, color)
    }

    @Test
    fun parseColorHex_handlesMalformedGracefully() {
        val fallback = Color(0xFF046B5E)
        val color = CategoryIcons.parseColorHex("invalid_hex", fallback)
        assertEquals(fallback, color)
    }
}
