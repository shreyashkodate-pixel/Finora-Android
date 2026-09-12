package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents monthly budget configurations per SRS §3.7 (BUD).
 * [categoryId] = null indicates overall monthly budget; non-null indicates per-category budget.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "yearMonth", "categoryId"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val yearMonth: String, // Format: YYYY-MM
    val categoryId: String? = null, // null for overall budget
    val amountMinorUnits: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
