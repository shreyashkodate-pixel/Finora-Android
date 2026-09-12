package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents expense categories per SRS §3.3 (CAT).
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
