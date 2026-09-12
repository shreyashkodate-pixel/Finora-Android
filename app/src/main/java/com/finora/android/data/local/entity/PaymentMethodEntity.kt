package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents payment methods/accounts per SRS §3.4 (PAY).
 */
@Entity(
    tableName = "payment_methods",
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
data class PaymentMethodEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val type: String, // CASH, UPI, DEBIT_CARD, CREDIT_CARD, BANK_TRANSFER, DIGITAL_WALLET, OTHER
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
