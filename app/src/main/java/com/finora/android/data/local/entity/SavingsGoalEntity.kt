package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a savings goal per SRS §3.24 (SVG).
 */
@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"])
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val targetAmountMinorUnits: Long,
    val currencyCode: String,
    val targetDate: Long? = null,
    val templateType: String = "CUSTOM", // EMERGENCY_FUND, VACATION, GADGET, CUSTOM
    val colorHex: String = "#4CAF50",
    val iconName: String = "Savings",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
