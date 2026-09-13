package com.finora.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents recurring subscriptions and fixed obligations per SRS §3.22 (REC).
 */
@Entity(
    tableName = "recurring_expenses",
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
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["nextDueDate"])
    ]
)
data class RecurringExpenseEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val title: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val categoryId: String,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: Long,
    val nextDueDate: Long,
    val accountId: String? = null,
    val isActive: Boolean = true,
    val autoLog: Boolean = false, // V1.2 requires manual confirmation by default
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
