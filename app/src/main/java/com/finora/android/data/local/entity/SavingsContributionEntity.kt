package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a deposit or withdrawal contribution to a savings goal per SRS §3.24 (SVG).
 */
@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["profileId"]),
        Index(value = ["contributionDate"])
    ]
)
data class SavingsContributionEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val profileId: String,
    val amountMinorUnits: Long,
    val contributionDate: Long,
    val type: String = "DEPOSIT", // DEPOSIT, WITHDRAWAL
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
