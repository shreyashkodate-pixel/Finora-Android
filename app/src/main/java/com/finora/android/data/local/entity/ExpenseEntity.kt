package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single financial expense record per SRS §3.2 (EXP) and DC-2/DC-8.
 */
@Entity(
    tableName = "expenses",
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
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "expenseDate"]),
        Index(value = ["categoryId"]),
        Index(value = ["paymentMethodId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val amountMinorUnits: Long, // Exact integer minor units per DC-2
    val currencyCode: String,   // Explicit transaction currency per DC-8
    val categoryId: String,
    val paymentMethodId: String? = null,
    val expenseDate: Long,      // Epoch millis for transaction date
    val title: String? = null,
    val notes: String? = null,
    val attachmentUri: String? = null,
    val source: String = "MANUAL",
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
