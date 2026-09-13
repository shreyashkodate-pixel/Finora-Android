package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents accounts and payment sources (Cash, Bank Account, Credit Card, etc.) per SRS §3.23 (ACC).
 */
@Entity(
    tableName = "accounts",
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
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val type: String, // CASH, BANK_ACCOUNT, DEBIT_CARD, CREDIT_CARD, DIGITAL_WALLET
    val initialBalanceMinorUnits: Long = 0L,
    val colorHex: String = "#1E88E5",
    val iconName: String = "AccountBalance",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
