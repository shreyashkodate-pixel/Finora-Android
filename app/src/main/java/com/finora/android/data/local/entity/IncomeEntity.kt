package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an income transaction per SRS §3.21 (INC).
 */
@Entity(
    tableName = "income",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["incomeDate"]),
        Index(value = ["accountId"])
    ]
)
data class IncomeEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val source: String, // Salary, Freelance, Investment, Gift, Refund, Other
    val incomeDate: Long,
    val accountId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
