package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the active local user profile per SRS §3.1 (PROF).
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val currencyCode: String,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
